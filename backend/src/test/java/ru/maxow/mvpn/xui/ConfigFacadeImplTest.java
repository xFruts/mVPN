package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.SubscriptionFormat;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficStateService;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.config.ConfigCacheService;
import ru.maxow.mvpn.xui.config.ConfigSyncService;
import ru.maxow.mvpn.xui.config.ConfigFacadeImpl;
import ru.maxow.mvpn.xui.dto.SubscriptionConfigPayload;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigFacadeImpl - Unit тесты")
class ConfigFacadeImplTest {

  @Mock
  private XuiPanelService xuiPanelService;
  @Mock
  private UserRepository userRepository;
  @Mock
  private SubscriptionRepository subscriptionRepository;
  @Mock
  private SubscriptionTrafficStateService trafficStateService;
  @Mock
  private ConfigCacheService configCacheService;
  @Mock
  private ConfigSyncService configSyncService;

  @InjectMocks
  private ConfigFacadeImpl configFacade;

  @BeforeEach
  void setUp() throws Exception {
    java.lang.reflect.Field field = ConfigFacadeImpl.class.getDeclaredField("objectMapper");
    field.setAccessible(true);
    field.set(configFacade, new ObjectMapper());

    SubscriptionTrafficState defaultTrafficState = new SubscriptionTrafficState();
    defaultTrafficState.setUsedBytes(0L);
    org.mockito.Mockito.lenient()
        .when(trafficStateService.syncTrafficForSubscription(any(User.class), any(Subscription.class)))
        .thenReturn(defaultTrafficState);

    org.mockito.Mockito.lenient()
        .when(configCacheService.get(anyLong()))
        .thenReturn(null);
    org.mockito.Mockito.lenient()
        .doNothing()
        .when(configSyncService)
        .asyncSyncSubscription(any(UUID.class));
  }

  @Test
  @DisplayName("Должен выбросить NotFoundException, если пользователь не найден")
  void shouldThrowWhenUserNotFound() {
    UUID code = UUID.randomUUID();
    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("Должен вернуть base64-конфиг, пропуская серверы с ошибками")
  void shouldReturnBase64ConfigAndSkipUnavailableServers() {
    UUID code = UUID.randomUUID();
    User user = user(100L, "ivanov");
    Server server1 = server(1L, "Moscow-1");
    Server server2 = server(2L, "SPB-1");
    Subscription subscription = activeSubscription(user, Set.of(server1, server2));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));
    doThrow(new XuiUnavailableException("xui temporarily unavailable"))
        .when(xuiPanelService).createOrUpdateClient(server1, user, subscription);
    doNothing().when(xuiPanelService).createOrUpdateClient(server2, user, subscription);
    when(xuiPanelService.getVlessConfig(server2, user)).thenReturn("vless://ok-2");

    SubscriptionConfigPayload payload = configFacade.getSubscriptionConfig(code);
    String decoded = new String(Base64.getDecoder().decode(payload.body()), StandardCharsets.UTF_8);

    assertThat(payload.format()).isEqualTo(SubscriptionFormat.VLESS);
    assertThat(decoded).isEqualTo("vless://ok-2");
  }

  private User user(Long id, String fullName) {
    User user = new User();
    user.setId(id);
    user.setFullName(fullName);
    return user;
  }

  private Server server(Long id, String name) {
    Server server = new Server();
    server.setId(id);
    server.setName(name);
    server.setStatus(ServerStatus.ACTIVE);
    return server;
  }

  private Subscription activeSubscription(User user, Set<Server> servers) {
    Tariff tariff = new Tariff();
    tariff.setServers(servers);
    tariff.setTrafficLimitGb(100);
    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setEndDate(OffsetDateTime.now().plusDays(30));
    subscription.setTariff(tariff);
    return subscription;
  }
}
