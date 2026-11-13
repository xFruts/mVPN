package ru.maxow.mvpn.subscription.traffic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.XuiClientTraffic;
import ru.maxow.mvpn.xui.XuiPanelService;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionTrafficStateServiceImpl - Unit Tests")
class SubscriptionTrafficStateServiceImplTest {

  @Mock
  XuiPanelService xuiPanelService;

  @Mock
  SubscriptionTrafficStateRepository trafficStateRepository;

  @Mock
  SubscriptionServerTrafficRepository serverTrafficRepository;

  @InjectMocks
  SubscriptionTrafficStateServiceImpl trafficStateService;

  User testUser;
  Subscription testSubscription;
  Tariff testTariff;
  Server serverA;
  Server serverB;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setFullName("ivan_testov");

    serverA = new Server();
    serverA.setId(10L);
    serverA.setName("Server-NL");
    serverA.setStatus(ServerStatus.ACTIVE);

    serverB = new Server();
    serverB.setId(20L);
    serverB.setName("Server-DE");
    serverB.setStatus(ServerStatus.ACTIVE);

    testTariff = new Tariff();
    testTariff.setId(100L);
    testTariff.setTrafficLimitGb(50);
    testTariff.setServers(Set.of(serverA, serverB));

    testSubscription = new Subscription();
    testSubscription.setId(2L);
    testSubscription.setUser(testUser);
    testSubscription.setTariff(testTariff);
    testSubscription.setStartDate(OffsetDateTime.now(ZoneOffset.UTC));
    testSubscription.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
  }

  @Test
  @DisplayName("1. Первый синк: ставит baseline серверов, использованный трафик = 0")
  void shouldInitializeBaselineOnFirstSync() {
    when(trafficStateRepository.findBySubscriptionId(2L))
        .thenReturn(Optional.empty());
    when(serverTrafficRepository.findBySubscriptionIdAndServerId(2L, 10L))
        .thenReturn(Optional.empty());
    when(serverTrafficRepository.findBySubscriptionIdAndServerId(2L, 20L))
        .thenReturn(Optional.empty());

    when(xuiPanelService.getClientTraffic(serverA, testUser.getFullName()))
        .thenReturn(new XuiClientTraffic(testUser.getFullName(), 1_000L, 5_000L, 6_000L, 0L));
    when(xuiPanelService.getClientTraffic(serverB, testUser.getFullName()))
        .thenReturn(new XuiClientTraffic(testUser.getFullName(), 2_000L, 8_000L, 10_000L, 0L));

    when(trafficStateRepository.save(any(SubscriptionTrafficState.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SubscriptionTrafficState state =
        trafficStateService.syncTrafficForSubscription(testUser, testSubscription);

    assertThat(state).isNotNull();
    assertThat(state.getUsedBytes()).isEqualTo(0L);
    assertThat(state.getUsedUploadBytes()).isEqualTo(0L);
    assertThat(state.getUsedDownloadBytes()).isEqualTo(0L);

    verify(serverTrafficRepository, times(2))
        .save(any(SubscriptionServerTraffic.class));
  }

  @Test
  @DisplayName("2. Рост трафика: дельты корректно суммируются с нескольких серверов")
  void shouldAccumulateDeltasAcrossMultipleServers() {
    SubscriptionTrafficState existingState = new SubscriptionTrafficState();
    existingState.setId(1L);
    existingState.setSubscriptionId(2L);
    existingState.setUsedBytes(1_000L);
    existingState.setUsedUploadBytes(200L);
    existingState.setUsedDownloadBytes(800L);
    existingState.setPeriodStartAt(Instant.now());

    SubscriptionServerTraffic stA = new SubscriptionServerTraffic();
    stA.setId(101L);
    stA.setSubscriptionId(2L);
    stA.setServerId(10L);
    stA.setLastUploadBytes(1_000L);
    stA.setLastDownloadBytes(5_000L);

    SubscriptionServerTraffic stB = new SubscriptionServerTraffic();
    stB.setId(102L);
    stB.setSubscriptionId(2L);
    stB.setServerId(20L);
    stB.setLastUploadBytes(2_000L);
    stB.setLastDownloadBytes(8_000L);

    when(trafficStateRepository.findBySubscriptionId(2L))
        .thenReturn(Optional.of(existingState));
    when(serverTrafficRepository.findBySubscriptionIdAndServerId(2L, 10L))
        .thenReturn(Optional.of(stA));
    when(serverTrafficRepository.findBySubscriptionIdAndServerId(2L, 20L))
        .thenReturn(Optional.of(stB));

    // Server A: +500 up, +1500 down -> +2000 bytes
    when(xuiPanelService.getClientTraffic(serverA, testUser.getFullName()))
        .thenReturn(new XuiClientTraffic(testUser.getFullName(), 1_500L, 6_500L, 8_000L, 0L));
    // Server B: +300 up, +700 down -> +1000 bytes
    when(xuiPanelService.getClientTraffic(serverB, testUser.getFullName()))
        .thenReturn(new XuiClientTraffic(testUser.getFullName(), 2_300L, 8_700L, 11_000L, 0L));

    when(trafficStateRepository.save(any(SubscriptionTrafficState.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SubscriptionTrafficState state =
        trafficStateService.syncTrafficForSubscription(testUser, testSubscription);

    // Initial 1000 + 2000 (from A) + 1000 (from B) = 4000
    assertThat(state.getUsedBytes()).isEqualTo(4_000L);
    assertThat(state.getUsedUploadBytes()).isEqualTo(200L + 500L + 300L);
    assertThat(state.getUsedDownloadBytes()).isEqualTo(800L + 1_500L + 700L);
  }

  @Test
  @DisplayName("3. Сброс ноды 3X-UI: не зависает в fallback, считает новую дельту от 0")
  void shouldHandleXuiResetGracefullyWithoutLosingTraffic() {
    SubscriptionTrafficState existingState = new SubscriptionTrafficState();
    existingState.setId(1L);
    existingState.setSubscriptionId(2L);
    existingState.setUsedBytes(50_000_000_000L); // 50 GB used
    existingState.setUsedUploadBytes(10_000_000_000L);
    existingState.setUsedDownloadBytes(40_000_000_000L);
    existingState.setPeriodStartAt(Instant.now());

    SubscriptionServerTraffic stA = new SubscriptionServerTraffic();
    stA.setId(101L);
    stA.setSubscriptionId(2L);
    stA.setServerId(10L);
    stA.setLastUploadBytes(10_000_000_000L);
    stA.setLastDownloadBytes(40_000_000_000L);

    testTariff.setServers(Set.of(serverA));

    when(trafficStateRepository.findBySubscriptionId(2L))
        .thenReturn(Optional.of(existingState));
    when(serverTrafficRepository.findBySubscriptionIdAndServerId(2L, 10L))
        .thenReturn(Optional.of(stA));

    // Server A was reset! Returns 100 MB up and 200 MB down (much less than previous 10GB/40GB)
    when(xuiPanelService.getClientTraffic(serverA, testUser.getFullName()))
        .thenReturn(new XuiClientTraffic(
            testUser.getFullName(), 100_000_000L, 200_000_000L, 300_000_000L, 0L));

    when(trafficStateRepository.save(any(SubscriptionTrafficState.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SubscriptionTrafficState state =
        trafficStateService.syncTrafficForSubscription(testUser, testSubscription);

    // Accumulated 50 GB + new delta (100MB + 200MB = 300MB) = 50.3 GB
    assertThat(state.getUsedBytes()).isEqualTo(50_300_000_000L);
    assertThat(stA.getLastUploadBytes()).isEqualTo(100_000_000L);
    assertThat(stA.getLastDownloadBytes()).isEqualTo(200_000_000L);
  }

  @Test
  @DisplayName("4. Наступление нового месяца: автоматически обнуляет счетчики периода")
  void shouldResetTrafficPeriodOnNewMonth() {
    SubscriptionTrafficState pastMonthState = new SubscriptionTrafficState();
    pastMonthState.setId(1L);
    pastMonthState.setSubscriptionId(2L);
    pastMonthState.setUsedBytes(270_221_670_526L); // The exact 270GB from user's log!
    pastMonthState.setUsedUploadBytes(50_000_000_000L);
    pastMonthState.setUsedDownloadBytes(220_221_670_526L);
    // Period start was in September 2026 (past month)
    pastMonthState.setPeriodStartAt(Instant.parse("2026-09-01T00:00:00Z"));

    testTariff.setServers(Set.of(serverA));

    when(trafficStateRepository.findBySubscriptionId(2L))
        .thenReturn(Optional.of(pastMonthState));
    when(serverTrafficRepository.findBySubscriptionIdAndServerId(2L, 10L))
        .thenReturn(Optional.empty());

    when(xuiPanelService.getClientTraffic(serverA, testUser.getFullName()))
        .thenReturn(new XuiClientTraffic(testUser.getFullName(), 0L, 0L, 0L, 0L));

    when(trafficStateRepository.save(any(SubscriptionTrafficState.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SubscriptionTrafficState state =
        trafficStateService.syncTrafficForSubscription(testUser, testSubscription);

    // Old 270GB wiped for the new month, resets to 0
    assertThat(state.getUsedBytes()).isEqualTo(0L);
    assertThat(state.getUsedUploadBytes()).isEqualTo(0L);
    assertThat(state.getUsedDownloadBytes()).isEqualTo(0L);
    assertThat(state.getPeriodStartAt()).isAfter(Instant.parse("2026-09-01T00:00:00Z"));
  }

  @Test
  @DisplayName("5. Ошибка всех серверов: возвращает кэшированное состояние, если есть")
  void shouldReturnCachedStateWhenAllServersFail() {
    SubscriptionTrafficState cachedState = new SubscriptionTrafficState();
    cachedState.setId(1L);
    cachedState.setSubscriptionId(2L);
    cachedState.setUsedBytes(12345L);
    cachedState.setPeriodStartAt(Instant.now());

    when(trafficStateRepository.findBySubscriptionId(2L))
        .thenReturn(Optional.of(cachedState));
    when(xuiPanelService.getClientTraffic(any(), any()))
        .thenThrow(new RuntimeException("Connection refused"));

    SubscriptionTrafficState state =
        trafficStateService.syncTrafficForSubscription(testUser, testSubscription);

    assertThat(state).isSameAs(cachedState);
    assertThat(state.getUsedBytes()).isEqualTo(12345L);
  }

  @Test
  @DisplayName("6. Ошибка всех серверов без кэша: выбрасывает XuiUnavailableException")
  void shouldThrowWhenAllServersFailAndNoCache() {
    when(trafficStateRepository.findBySubscriptionId(2L))
        .thenReturn(Optional.empty());
    when(xuiPanelService.getClientTraffic(any(), any()))
        .thenThrow(new RuntimeException("Connection refused"));

    assertThatThrownBy(() ->
        trafficStateService.syncTrafficForSubscription(testUser, testSubscription))
        .isInstanceOf(XuiUnavailableException.class);
  }

  @Test
  @DisplayName("7. resetTrafficPeriod: принудительно обнуляет состояние подписки")
  void shouldResetTrafficPeriodManually() {
    SubscriptionTrafficState state = new SubscriptionTrafficState();
    state.setId(1L);
    state.setSubscriptionId(2L);
    state.setUsedBytes(9999L);
    state.setPeriodStartAt(Instant.now());

    when(trafficStateRepository.findBySubscriptionId(2L))
        .thenReturn(Optional.of(state));
    when(trafficStateRepository.save(any(SubscriptionTrafficState.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    SubscriptionTrafficState result = trafficStateService.resetTrafficPeriod(2L);

    assertThat(result.getUsedBytes()).isEqualTo(0L);
    assertThat(result.getUsedUploadBytes()).isEqualTo(0L);
    assertThat(result.getUsedDownloadBytes()).isEqualTo(0L);
  }

  @Test
  @DisplayName("8. isLimitExceeded: безлимит (0 или отрицательный) никогда не превышен")
  void shouldHandleUnlimitedTrafficCorrectly() {
    SubscriptionTrafficState state = new SubscriptionTrafficState();
    state.setUsedBytes(500_000_000_000L); // 500 GB

    assertThat(state.isLimitExceeded(0L)).isFalse();
    assertThat(state.isLimitExceeded(-1L)).isFalse();
    assertThat(state.isLimitExceeded(100_000_000_000L)).isTrue();
  }

  @Test
  @DisplayName("9. resetAllExpiredPeriods: обнуляет только подписки с истекшим месяцем")
  void shouldResetOnlyExpiredPeriodsInScheduledJob() {
    SubscriptionTrafficState expiredState = new SubscriptionTrafficState();
    expiredState.setId(1L);
    expiredState.setSubscriptionId(10L);
    expiredState.setUsedBytes(50_000_000_000L);
    expiredState.setPeriodStartAt(Instant.parse("2026-08-01T00:00:00Z"));

    SubscriptionTrafficState currentState = new SubscriptionTrafficState();
    currentState.setId(2L);
    currentState.setSubscriptionId(20L);
    currentState.setUsedBytes(10_000_000_000L);
    currentState.setPeriodStartAt(Instant.now());

    when(trafficStateRepository.findAll()).thenReturn(List.of(expiredState, currentState));
    when(trafficStateRepository.findBySubscriptionId(10L)).thenReturn(Optional.of(expiredState));
    when(trafficStateRepository.save(any(SubscriptionTrafficState.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    trafficStateService.resetAllExpiredPeriods();

    verify(trafficStateRepository).findBySubscriptionId(10L);
    verify(trafficStateRepository, times(0)).findBySubscriptionId(20L);
    assertThat(expiredState.getUsedBytes()).isEqualTo(0L);
    assertThat(currentState.getUsedBytes()).isEqualTo(10_000_000_000L);
  }
}
