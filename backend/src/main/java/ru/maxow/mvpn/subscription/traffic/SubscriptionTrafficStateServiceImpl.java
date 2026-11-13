package ru.maxow.mvpn.subscription.traffic;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.XuiClientTraffic;
import ru.maxow.mvpn.xui.XuiPanelService;
import ru.maxow.mvpn.xui.dto.TrafficSyncSource;

/**
 * Реализация сервиса синхронизации и учета трафика подписок.
 * <p>
 * Бэкенд выступает как единый источник истины (Single Source of Truth, SSOT):
 * 1. Сохраняет суммарный использованный трафик за текущий расчетный период (месяц).
 * 2. С серверов 3X-UI считывает локальные дельты (Rate/Counter ingestion pattern).
 *    При сбросе ноды 3X-UI накопленный трафик на бэкенде сохраняется, а новые байты считаются от нуля.
 * 3. Автоматически обнуляет использованный трафик при наступлении нового расчетного месяца.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionTrafficStateServiceImpl implements SubscriptionTrafficStateService {

  XuiPanelService xuiPanelService;
  SubscriptionTrafficStateRepository trafficStateRepository;
  SubscriptionServerTrafficRepository serverTrafficRepository;

  @Override
  @Transactional
  public SubscriptionTrafficState syncTrafficForSubscription(
      User user,
      Subscription subscription) throws XuiUnavailableException {

    Instant now = Instant.now();
    Long subId = subscription.getId();

    SubscriptionTrafficState state = trafficStateRepository.findBySubscriptionId(subId)
        .orElseGet(() -> createInitialState(subId, now));

    boolean isNewPeriod = shouldResetPeriod(state.getPeriodStartAt(), now);
    if (isNewPeriod) {
      log.info("Starting new traffic billing period for subscriptionId={}. Previous usedBytes={}",
          subId, state.getUsedBytes());
      resetStateCounters(state, now);
    }

    int successfulServers = 0;
    long deltaUploadTotal = 0L;
    long deltaDownloadTotal = 0L;
    long totalServerBytes = 0L;

    for (Server server : subscription.getTariff().getServers()) {
      if (server.getStatus() != ServerStatus.ACTIVE) {
        continue;
      }

      XuiClientTraffic traffic;
      try {
        traffic = xuiPanelService.getClientTraffic(server, user.getFullName());
      } catch (Exception e) {
        log.warn("Failed to get traffic from server {} (id={}) for user {}: {}",
            server.getName(), server.getId(), user.getId(), e.getMessage());
        continue;
      }

      if (traffic == null) {
        continue;
      }

      successfulServers++;
      long currentUp = traffic.upload();
      long currentDown = traffic.download();
      totalServerBytes += (currentUp + currentDown);

      SubscriptionServerTraffic serverTraffic = serverTrafficRepository
          .findBySubscriptionIdAndServerId(subId, server.getId())
          .orElseGet(() -> createInitialServerTraffic(subId, server.getId(), now));

      long deltaUp = 0L;
      long deltaDown = 0L;

      if (isNewPeriod || serverTraffic.getId() == null) {
        // Re-anchor baseline for the new period or newly attached server
        serverTraffic.setLastUploadBytes(currentUp);
        serverTraffic.setLastDownloadBytes(currentDown);
      } else {
        if (currentUp >= serverTraffic.getLastUploadBytes()) {
          deltaUp = currentUp - serverTraffic.getLastUploadBytes();
        } else {
          log.info("Server upload counter reset detected on server {} for subId={}. previous={}, current={}",
              server.getName(), subId, serverTraffic.getLastUploadBytes(), currentUp);
          deltaUp = currentUp;
        }

        if (currentDown >= serverTraffic.getLastDownloadBytes()) {
          deltaDown = currentDown - serverTraffic.getLastDownloadBytes();
        } else {
          log.info("Server download counter reset detected on server {} for subId={}. previous={}, current={}",
              server.getName(), subId, serverTraffic.getLastDownloadBytes(), currentDown);
          deltaDown = currentDown;
        }

        serverTraffic.setLastUploadBytes(currentUp);
        serverTraffic.setLastDownloadBytes(currentDown);
      }

      serverTraffic.setLastSyncedAt(now);
      serverTrafficRepository.save(serverTraffic);

      deltaUploadTotal += deltaUp;
      deltaDownloadTotal += deltaDown;
    }

    if (successfulServers == 0) {
      log.warn("No servers responded with traffic data for subId={}", subId);
      if (state.getId() != null) {
        return state;
      }
      throw new XuiUnavailableException("Cannot sync traffic: all servers unavailable and no cached data");
    }

    // Auto-reconciliation: If legacy state had huge traffic (e.g. from previous bugs)
    // and no server traffic was ever recorded, but current servers are empty (0 bytes),
    // align state with reality.
    if (!isNewPeriod && totalServerBytes == 0L && serverTrafficRepository.findBySubscriptionId(subId).isEmpty()) {
      log.info("Reconciling traffic state with zeroed servers for subId={}", subId);
      resetStateCounters(state, now);
    } else if (!isNewPeriod) {
      state.setUsedUploadBytes(state.getEffectiveUploadBytes() + deltaUploadTotal);
      state.setUsedDownloadBytes(state.getEffectiveDownloadBytes() + deltaDownloadTotal);
      state.setUsedBytes(state.getEffectiveUploadBytes() + state.getEffectiveDownloadBytes());
    }

    state.setLastSyncedAt(now);
    state.setSource(TrafficSyncSource.XUI_SYNC);

    SubscriptionTrafficState saved = trafficStateRepository.save(state);
    log.debug("Traffic synced successfully for subId={}. usedBytes={}, servers={}",
        subId, saved.getUsedBytes(), successfulServers);

    return saved;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<SubscriptionTrafficState> getTrafficStateBySubscriptionId(Long subscriptionId) {
    return trafficStateRepository.findBySubscriptionId(subscriptionId);
  }

  @Override
  @Transactional
  public SubscriptionTrafficState resetTrafficPeriod(Long subscriptionId) {
    Instant now = Instant.now();
    SubscriptionTrafficState state = trafficStateRepository.findBySubscriptionId(subscriptionId)
        .orElseGet(() -> createInitialState(subscriptionId, now));

    resetStateCounters(state, now);
    state.setLastSyncedAt(now);
    state.setSource(TrafficSyncSource.XUI_SYNC);

    List<SubscriptionServerTraffic> serverTraffics =
        serverTrafficRepository.findBySubscriptionId(subscriptionId);
    for (SubscriptionServerTraffic st : serverTraffics) {
      st.setLastUploadBytes(0L);
      st.setLastDownloadBytes(0L);
      st.setLastSyncedAt(now);
    }
    serverTrafficRepository.saveAll(serverTraffics);

    log.info("Traffic period reset for subscriptionId={}", subscriptionId);
    return trafficStateRepository.save(state);
  }

  @Override
  @Transactional
  @Scheduled(cron = "0 0 * * * ?")
  public void resetAllExpiredPeriods() {
    Instant now = Instant.now();
    List<SubscriptionTrafficState> allStates = trafficStateRepository.findAll();
    for (SubscriptionTrafficState state : allStates) {
      if (shouldResetPeriod(state.getPeriodStartAt(), now)) {
        log.info("Periodic job resetting expired traffic period for subscriptionId={}",
            state.getSubscriptionId());
        resetTrafficPeriod(state.getSubscriptionId());
      }
    }
  }

  private boolean shouldResetPeriod(Instant periodStartAt, Instant now) {
    if (periodStartAt == null) {
      return true;
    }
    YearMonth periodMonth = YearMonth.from(periodStartAt.atZone(ZoneOffset.UTC));
    YearMonth currentMonth = YearMonth.from(now.atZone(ZoneOffset.UTC));
    return currentMonth.isAfter(periodMonth);
  }

  private void resetStateCounters(SubscriptionTrafficState state, Instant now) {
    state.setUsedBytes(0L);
    state.setUsedUploadBytes(0L);
    state.setUsedDownloadBytes(0L);
    state.setPeriodStartTotalBytes(0L);
    state.setPeriodStartUploadBytes(0L);
    state.setPeriodStartDownloadBytes(0L);
    state.setPeriodStartAt(now);
  }

  private SubscriptionTrafficState createInitialState(Long subscriptionId, Instant now) {
    SubscriptionTrafficState state = new SubscriptionTrafficState();
    state.setSubscriptionId(subscriptionId);
    resetStateCounters(state, now);
    state.setLastSyncedAt(now);
    state.setSource(TrafficSyncSource.XUI_SYNC);
    return state;
  }

  private SubscriptionServerTraffic createInitialServerTraffic(
      Long subscriptionId, Long serverId, Instant now) {
    SubscriptionServerTraffic st = new SubscriptionServerTraffic();
    st.setSubscriptionId(subscriptionId);
    st.setServerId(serverId);
    st.setLastUploadBytes(0L);
    st.setLastDownloadBytes(0L);
    st.setLastSyncedAt(now);
    return st;
  }
}
