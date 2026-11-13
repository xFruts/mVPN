package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.SubscriptionFormat;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.config.ConfigCacheService;
import ru.maxow.mvpn.xui.config.ConfigSyncService;
import ru.maxow.mvpn.xui.dto.SubscriptionConfigPayload;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigSyncService - Unit тесты")
class ConfigSyncServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private SubscriptionRepository subscriptionRepository;
  @Mock private XuiPanelService xuiPanelService;
  @Mock private ConfigCacheService configCacheService;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks
  private ConfigSyncService configSyncService;

  @BeforeEach
  void setUp() {
    configSyncService = new ConfigSyncService(
        userRepository,
        subscriptionRepository,
        xuiPanelService,
        configCacheService,
        objectMapper
    );
  }

  @Test
  @DisplayName("Должен успешно обновить оставшиеся серверы, если один сервер недоступен")
  void shouldSyncRemainingServersWhenOneFails() {
    UUID code = UUID.randomUUID();
    User user = user(1L, "user1");
    Server server1 = server(11L, "Netherlands", ServerStatus.ACTIVE, SubscriptionFormat.VLESS);
    Server server2 = server(2L, "Germany", ServerStatus.ACTIVE, SubscriptionFormat.VLESS);
    Subscription subscription = subscription(user, Set.of(server1, server2));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository
        .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));

    doThrow(new XuiUnavailableException("Cannot connect to server: Netherlands"))
        .when(xuiPanelService).createOrUpdateClient(server1, user, subscription);
    doNothing().when(xuiPanelService).createOrUpdateClient(server2, user, subscription);
    when(xuiPanelService.getVlessConfig(server2, user)).thenReturn("vless://germany");

    configSyncService.asyncSyncSubscription(code);

    verify(xuiPanelService, never()).getVlessConfig(server1, user);
    verify(configCacheService).put(eq(user.getId()), argThat(payload -> {
      assertThat(payload.format()).isEqualTo(SubscriptionFormat.VLESS);
      String decoded = new String(
          Base64.getDecoder().decode(payload.body()), StandardCharsets.UTF_8);
      assertThat(decoded).isEqualTo("vless://germany");
      return true;
    }));
  }

  @Test
  @DisplayName("Должен пропускать неактивные серверы")
  void shouldSkipInactiveServers() {
    UUID code = UUID.randomUUID();
    User user = user(2L, "user2");
    Server activeServer =
        server(2L, "ActiveServer", ServerStatus.ACTIVE, SubscriptionFormat.VLESS);
    Server inactiveServer =
        server(3L, "DisabledServer", ServerStatus.INACTIVE, SubscriptionFormat.VLESS);
    Subscription subscription = subscription(user, Set.of(activeServer, inactiveServer));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository
        .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));

    doNothing().when(xuiPanelService).createOrUpdateClient(activeServer, user, subscription);
    when(xuiPanelService.getVlessConfig(activeServer, user)).thenReturn("vless://active");

    configSyncService.asyncSyncSubscription(code);

    verify(xuiPanelService, never()).createOrUpdateClient(inactiveServer, user, subscription);
    verify(xuiPanelService, never()).getVlessConfig(inactiveServer, user);
    verify(configCacheService).put(eq(user.getId()), any(SubscriptionConfigPayload.class));
  }

  @Test
  @DisplayName("Не должен обновлять кэш, если все серверы завершились с ошибкой")
  void shouldNotUpdateCacheWhenAllServersFail() {
    UUID code = UUID.randomUUID();
    User user = user(3L, "user3");
    Server server1 = server(11L, "Netherlands", ServerStatus.ACTIVE, SubscriptionFormat.VLESS);
    Subscription subscription = subscription(user, Set.of(server1));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository
        .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));

    doThrow(new XuiUnavailableException("Cannot connect"))
        .when(xuiPanelService).createOrUpdateClient(server1, user, subscription);

    configSyncService.asyncSyncSubscription(code);

    verify(configCacheService, never()).put(any(), any());
  }

  @Test
  @DisplayName("Должен корректно объединять JSON конфиги в массив")
  void shouldCombineJsonConfigsInArray() {
    UUID code = UUID.randomUUID();
    User user = user(4L, "user4");
    Server server1 = server(1L, "Server1", ServerStatus.ACTIVE, SubscriptionFormat.JSON);
    Server server2 = server(2L, "Server2", ServerStatus.ACTIVE, SubscriptionFormat.JSON);
    Subscription subscription = subscription(user, Set.of(server1, server2));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository
        .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));

    doNothing().when(xuiPanelService).createOrUpdateClient(server1, user, subscription);
    doNothing().when(xuiPanelService).createOrUpdateClient(server2, user, subscription);
    when(xuiPanelService.getJsonConfig(server1, user)).thenReturn("{\"tag\":\"server1\"}");
    when(xuiPanelService.getJsonConfig(server2, user)).thenReturn("{\"tag\":\"server2\"}");

    configSyncService.asyncSyncSubscription(code);

    verify(configCacheService).put(eq(user.getId()), argThat(payload -> {
      assertThat(payload.format()).isEqualTo(SubscriptionFormat.JSON);
      assertThat(payload.body()).contains("{\"tag\":\"server1\"}");
      assertThat(payload.body()).contains("{\"tag\":\"server2\"}");
      return true;
    }));
  }

  private User user(Long id, String username) {
    User user = new User();
    user.setId(id);
    user.setFullName(username);
    user.setXuiId(UUID.randomUUID());
    user.setXuiSubscription(UUID.randomUUID());
    return user;
  }

  private Server server(Long id, String name, ServerStatus status, SubscriptionFormat format) {
    Server server = new Server();
    server.setId(id);
    server.setName(name);
    server.setStatus(status);
    server.setSubscriptionFormat(format);
    server.setIp("1.2.3.4");
    server.setCountryEmoji("NL");
    return server;
  }

  private Subscription subscription(User user, Set<Server> servers) {
    Tariff tariff = new Tariff();
    tariff.setServers(servers);
    tariff.setTrafficLimitGb(100);

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setTariff(tariff);
    subscription.setEndDate(OffsetDateTime.now().plusDays(30));
    return subscription;
  }
}
