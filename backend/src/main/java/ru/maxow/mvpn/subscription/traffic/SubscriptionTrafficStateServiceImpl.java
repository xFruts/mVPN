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
import java.time.YearMonth;
import java.time.ZoneId;
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
    Instant now = Instant.now();

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
      Long previousPeriodStartTotal = previous.getPeriodStartTotalBytes();
      if (previousPeriodStartTotal != null) {
        long previousTotal = previousPeriodStartTotal + previous.getUsedBytes();
        if (!isTrafficDataValid(totalUsedBytes, previousTotal)) {
          log.warn(
              "New traffic data looks invalid. subscriptionId={}, previous={} bytes, new={} bytes. Using fallback.",
              subscription.getId(), previousTotal, totalUsedBytes
          );
          return previous;
        }
      }
    }

    SubscriptionTrafficState state = previousOpt.orElse(
        new SubscriptionTrafficState()
    );

    Long periodStartTotal = previousOpt.map(SubscriptionTrafficState::getPeriodStartTotalBytes).orElse(null);
    Long periodStartUpload = previousOpt.map(SubscriptionTrafficState::getPeriodStartUploadBytes).orElse(null);
    Long periodStartDownload = previousOpt.map(SubscriptionTrafficState::getPeriodStartDownloadBytes).orElse(null);
    Instant periodStartAt = previousOpt.map(SubscriptionTrafficState::getPeriodStartAt).orElse(null);

    boolean resetPeriod = shouldResetPeriod(periodStartAt, now);
    if (resetPeriod || periodStartTotal == null || periodStartUpload == null || periodStartDownload == null) {
      periodStartTotal = totalUsedBytes;
      periodStartUpload = totalUploadBytes;
      periodStartDownload = totalDownloadBytes;
      periodStartAt = now;
    }

    long usedBytes = totalUsedBytes - periodStartTotal;
    long usedUploadBytes = totalUploadBytes - periodStartUpload;
    long usedDownloadBytes = totalDownloadBytes - periodStartDownload;

    if (usedBytes < 0 || usedUploadBytes < 0 || usedDownloadBytes < 0) {
      periodStartTotal = totalUsedBytes;
      periodStartUpload = totalUploadBytes;
      periodStartDownload = totalDownloadBytes;
      periodStartAt = now;
      usedBytes = 0L;
      usedUploadBytes = 0L;
      usedDownloadBytes = 0L;
    }

    state.setSubscriptionId(subscription.getId());
    state.setUsedBytes(usedBytes);
    state.setUsedUploadBytes(usedUploadBytes);
    state.setUsedDownloadBytes(usedDownloadBytes);
    state.setPeriodStartTotalBytes(periodStartTotal);
    state.setPeriodStartUploadBytes(periodStartUpload);
    state.setPeriodStartDownloadBytes(periodStartDownload);
    state.setPeriodStartAt(periodStartAt);
    state.setLastSyncedAt(now);
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
    long tolerance = 100 * 1024 * 1024; // 100 MB tolerance
    return !(newBytes < (previousBytes - tolerance));
  }

  private boolean shouldResetPeriod(Instant periodStartAt, Instant now) {
    if (periodStartAt == null) {
      return true;
    }
    ZoneId zoneId = ZoneId.systemDefault();
    YearMonth periodMonth = YearMonth.from(periodStartAt.atZone(zoneId));
    YearMonth currentMonth = YearMonth.from(now.atZone(zoneId));
    return currentMonth.isAfter(periodMonth);
  }
}
