package ru.maxow.mvpn.subscription.traffic;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.dto.TrafficSyncSource;
import ru.maxow.mvpn.xui.XuiClientTraffic;
import ru.maxow.mvpn.xui.XuiPanelService;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionTrafficStateServiceImpl implements SubscriptionTrafficStateService {

  static long TRAFFIC_TOLERANCE_BYTES = 100L * 1024L * 1024L; // 100 MB

  XuiPanelService xuiPanelService;
  SubscriptionTrafficStateRepository trafficStateRepository;

  @Override
  public SubscriptionTrafficState syncTrafficForSubscription(
      User user,
      Subscription subscription) throws XuiUnavailableException {

    TrafficAccumulator accumulator = fetchTrafficFromServers(user, subscription);

    Optional<SubscriptionTrafficState> previousOpt =
        trafficStateRepository.findBySubscriptionId(subscription.getId());

    if (accumulator.successfulServers == 0) {
      log.warn("No servers responded with traffic data. userId={}, subscriptionId={}",
          user.getId(), subscription.getId());

      return previousOpt.orElseThrow(() -> new XuiUnavailableException(
          "Cannot sync traffic: all servers unavailable and no cached data"));
    }

    if (previousOpt.isPresent() && previousOpt.get().getPeriodStartTotalBytes() != null) {
      SubscriptionTrafficState previous = previousOpt.get();
      long previousTotal = previous.getPeriodStartTotalBytes() + previous.getUsedBytes();

      if (isTrafficAnomaly(accumulator.totalBytes(), previousTotal)) {
        log.warn("New traffic data looks invalid (possible XUI reset)." +
                " subId={}, previous={} bytes, new={} bytes. Using fallback.",
            subscription.getId(), previousTotal, accumulator.totalBytes());
        return previous;
      }
    }

    SubscriptionTrafficState newState = calculateNewState(
        subscription.getId(), accumulator, previousOpt.orElse(null));

    SubscriptionTrafficState saved = trafficStateRepository.save(newState);

    log.info("Traffic synced successfully." +
            " subscriptionId={}, upload={} b, download={} b, total={} b, servers={}/{}",
        subscription.getId(), accumulator.upload, accumulator.download, accumulator.totalBytes(),
        accumulator.successfulServers, accumulator.totalServers);

    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SubscriptionTrafficState> getTrafficStateBySubscriptionId(Long subscriptionId) {
    return trafficStateRepository.findBySubscriptionId(subscriptionId);
  }

  private TrafficAccumulator fetchTrafficFromServers(User user, Subscription subscription) {
    long totalUpload = 0L;
    long totalDownload = 0L;
    int successfulServers = 0;
    int totalServers = 0;

    for (Server server : subscription.getTariff().getServers()) {
      if (server.getStatus() == ServerStatus.ACTIVE) {
        totalServers++;
        try {
          XuiClientTraffic traffic = xuiPanelService.getClientTraffic(server, user.getFullName());
          if (traffic != null) {
            totalUpload += traffic.upload();
            totalDownload += traffic.download();
            successfulServers++;
          }
        } catch (Exception e) {
          log.warn("Failed to get traffic from server {}. userId={}, xuiId={}, serverId={}",
              server.getName(), user.getId(), user.getXuiId(), server.getId(), e);
        }
      }
    }
    return new TrafficAccumulator(totalUpload, totalDownload, successfulServers, totalServers);
  }

  private SubscriptionTrafficState calculateNewState(
      Long subId,
      TrafficAccumulator accumulator,
      SubscriptionTrafficState previousState) {

    Instant now = Instant.now();

    SubscriptionTrafficState state = previousState != null
        ? previousState : new SubscriptionTrafficState();

    Long periodStartTotal = state.getPeriodStartTotalBytes();
    Long periodStartUpload = state.getPeriodStartUploadBytes();
    Long periodStartDownload = state.getPeriodStartDownloadBytes();
    Instant periodStartAt = state.getPeriodStartAt();

    boolean isNewPeriod = shouldResetPeriod(periodStartAt, now);

    if (isNewPeriod || periodStartTotal == null
        || periodStartUpload == null || periodStartDownload == null) {
      periodStartTotal = accumulator.totalBytes();
      periodStartUpload = accumulator.upload;
      periodStartDownload = accumulator.download;
      periodStartAt = now;
    }

    long usedBytes = accumulator.totalBytes() - periodStartTotal;
    long usedUploadBytes = accumulator.upload - periodStartUpload;
    long usedDownloadBytes = accumulator.download - periodStartDownload;

    if (usedBytes < 0 || usedUploadBytes < 0 || usedDownloadBytes < 0) {
      periodStartTotal = accumulator.totalBytes();
      periodStartUpload = accumulator.upload;
      periodStartDownload = accumulator.download;
      periodStartAt = now;
      usedBytes = 0L;
      usedUploadBytes = 0L;
      usedDownloadBytes = 0L;
    }

    state.setSubscriptionId(subId);
    state.setUsedBytes(usedBytes);
    state.setUsedUploadBytes(usedUploadBytes);
    state.setUsedDownloadBytes(usedDownloadBytes);
    state.setPeriodStartTotalBytes(periodStartTotal);
    state.setPeriodStartUploadBytes(periodStartUpload);
    state.setPeriodStartDownloadBytes(periodStartDownload);
    state.setPeriodStartAt(periodStartAt);
    state.setLastSyncedAt(now);
    state.setSource(TrafficSyncSource.XUI_SYNC);

    return state;
  }

  private boolean isTrafficAnomaly(long newBytes, long previousBytes) {
    return newBytes < (previousBytes - TRAFFIC_TOLERANCE_BYTES);
  }

  private boolean shouldResetPeriod(Instant periodStartAt, Instant now) {
    if (periodStartAt == null) {
      return true;
    }
    YearMonth periodMonth = YearMonth.from(periodStartAt.atZone(ZoneOffset.UTC));
    YearMonth currentMonth = YearMonth.from(now.atZone(ZoneOffset.UTC));
    return currentMonth.isAfter(periodMonth);
  }

  private record TrafficAccumulator(long upload, long download,
                                    int successfulServers, int totalServers) {
    public long totalBytes() {
      return upload + download;
    }
  }
}
