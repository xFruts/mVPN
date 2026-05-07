package ru.maxow.mvpn.subscription.traffic;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.TrafficSyncSource;
import ru.maxow.mvpn.xui.XuiClientTraffic;
import ru.maxow.mvpn.xui.XuiPanelService;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionTrafficStateServiceImpl implements SubscriptionTrafficStateService {

  XuiPanelService xuiPanelService;
  SubscriptionTrafficStateRepository trafficStateRepository;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SubscriptionTrafficState syncTrafficForSubscription(
      User user,
      Subscription subscription) throws XuiUnavailableException {

    long totalUploadBytes = 0L;
    long totalDownloadBytes = 0L;
    TrafficSyncSource source = TrafficSyncSource.XUI_SYNC;
    int successfulServers = 0;
    int totalServers = 0;

    var servers = subscription.getTariff().getServers();

    for (Server server : servers) {
      if (server.getStatus().name().equals("ACTIVE")) {
        totalServers++;
        try {
          XuiClientTraffic traffic = xuiPanelService.getClientTraffic(
              server,
              user.getXuiId().toString()
          );

          if (traffic != null) {
            totalUploadBytes += traffic.getUpload() != null ? traffic.getUpload() : 0L;
            totalDownloadBytes += traffic.getDownload() != null ? traffic.getDownload() : 0L;
            successfulServers++;
          }
        } catch (Exception e) {
          log.warn(
              "Failed to get traffic from server {}. userId={}, xuiId={}, serverId={}",
              server.getName(), user.getId(), user.getXuiId(), server.getId(), e
          );
        }
      }
    }

    long totalUsedBytes = totalUploadBytes + totalDownloadBytes;

    if (successfulServers == 0) {
      log.warn("No servers responded with traffic data. userId={}, subscriptionId={}",
          user.getId(), subscription.getId());
      return trafficStateRepository.findBySubscriptionId(subscription.getId())
          .orElseThrow(() -> new XuiUnavailableException(
              "Cannot sync traffic: all servers unavailable and no cached data"));
    }

    Optional<SubscriptionTrafficState> previousOpt =
        trafficStateRepository.findBySubscriptionId(subscription.getId());

    if (previousOpt.isPresent()) {
      SubscriptionTrafficState previous = previousOpt.get();
      if (!isTrafficDataValid(totalUsedBytes, previous.getUsedBytes())) {
        log.warn(
            "New traffic data looks invalid. subscriptionId={}, previous={} bytes, new={} bytes. Using fallback.",
            subscription.getId(), previous.getUsedBytes(), totalUsedBytes
        );
        return previous;
      }
    }

    SubscriptionTrafficState state = previousOpt.orElse(
        new SubscriptionTrafficState()
    );
    state.setSubscriptionId(subscription.getId());
    state.setUsedBytes(totalUsedBytes);
    state.setUsedUploadBytes(totalUploadBytes);
    state.setUsedDownloadBytes(totalDownloadBytes);
    state.setLastSyncedAt(Instant.now());
    state.setSource(source);

    SubscriptionTrafficState saved = trafficStateRepository.save(state);

    log.info(
        "Traffic synced successfully. subscriptionId={}, upload={} bytes, download={} bytes, total={} bytes, servers={}/{}",
        subscription.getId(), totalUploadBytes, totalDownloadBytes, totalUsedBytes,
        successfulServers, totalServers
    );

    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SubscriptionTrafficState> getTrafficStateBySubscriptionId(Long subscriptionId) {
    return trafficStateRepository.findBySubscriptionId(subscriptionId);
  }

  private boolean isTrafficDataValid(long newBytes, long previousBytes) {
    long tolerance = 100 * 1024 * 1024; // 100 МБ допуска
    return !(newBytes < (previousBytes - tolerance));
  }
}
