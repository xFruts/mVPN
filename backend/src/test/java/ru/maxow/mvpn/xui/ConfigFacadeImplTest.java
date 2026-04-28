package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.core.JsonProcessingException;

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

  @InjectMocks
  private ConfigFacadeImpl configFacade;

  @BeforeEach
  void setUp() {
    // Инициализируем реальный ObjectMapper в ConfigFacadeImpl используя reflection
    try {
      java.lang.reflect.Field field = ConfigFacadeImpl.class.getDeclaredField("objectMapper");
      field.setAccessible(true);
      field.set(configFacade, new ObjectMapper());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Mock default traffic state to return empty traffic (no usage)
    // Use lenient() to avoid UnnecessaryStubbing errors in tests that don't use trafficStateService
    SubscriptionTrafficState defaultTrafficState = new SubscriptionTrafficState();
    defaultTrafficState.setUsedBytes(0L);
    defaultTrafficState.setUsedUploadBytes(0L);
    defaultTrafficState.setUsedDownloadBytes(0L);
    org.mockito.Mockito.lenient()
        .when(trafficStateService.syncTrafficForSubscription(
            org.mockito.ArgumentMatchers.any(User.class),
            org.mockito.ArgumentMatchers.any(Subscription.class)))
        .thenReturn(defaultTrafficState);
  }

  @Test
  @DisplayName("Должен выбросить NotFoundException, если пользователь по verificationCode не найден")
  void shouldThrowWhenUserNotFoundByVerificationCode() {
    UUID code = UUID.randomUUID();
    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(NotFoundException.class)
        .satisfies(error -> {
          NotFoundException ex = (NotFoundException) error;
          assertThat(ex.getEntityName()).isEqualTo("User by verification code");
          assertThat(ex.getIdentifier()).isNull();
        });
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
    when(xuiPanelService.getVlessConfig(server1, user))
        .thenThrow(new XuiUnavailableException("xui temporarily unavailable"));
    when(xuiPanelService.getVlessConfig(server2, user)).thenReturn("vless://ok-2");

    SubscriptionConfigPayload payload = configFacade.getSubscriptionConfig(code);
    String decoded = new String(Base64.getDecoder().decode(payload.body()), StandardCharsets.UTF_8);

    assertThat(payload.format()).isEqualTo(SubscriptionFormat.VLESS);
    assertThat(decoded).isEqualTo("vless://ok-2");
  }

  @Test
  @DisplayName("Должен вернуть JSON payload без base64, если все серверы настроены на JSON")
  void shouldReturnJsonPayloadWhenServersUseJsonFormat() {
    UUID code = UUID.randomUUID();
    User user = user(500L, "json-user");
    Server server = server(9L, "JSON-1");
    server.setSubscriptionFormat(SubscriptionFormat.JSON);
    Subscription subscription = activeSubscription(user, Set.of(server));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));
    when(xuiPanelService.getJsonConfig(server, user)).thenReturn("{\"format\":\"json\",\"remarks\":\"JSON-1\"}");

    SubscriptionConfigPayload payload = configFacade.getSubscriptionConfig(code);

    assertThat(payload.format()).isEqualTo(SubscriptionFormat.JSON);
    // Payload должен быть JSON массив с одним элементом
    assertThat(payload.body()).isEqualTo("[{\"format\":\"json\",\"remarks\":\"JSON-1\"}]");
  }

  @Test
  @DisplayName("Должен вернуть JSON массив с несколькими конфигами")
  void shouldReturnJsonArrayWithMultipleConfigs() throws JsonProcessingException {
    UUID code = UUID.randomUUID();
    User user = user(700L, "multi-json-user");
    Server server1 = server(10L, "Server-1");
    server1.setSubscriptionFormat(SubscriptionFormat.JSON);
    Server server2 = server(11L, "Server-2");
    server2.setSubscriptionFormat(SubscriptionFormat.JSON);
    Subscription subscription = activeSubscription(user, Set.of(server1, server2));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));
    when(xuiPanelService.getJsonConfig(server1, user)).thenReturn("{\"format\":\"json\",\"remarks\":\"Server-1\"}");
    when(xuiPanelService.getJsonConfig(server2, user)).thenReturn("{\"format\":\"json\",\"remarks\":\"Server-2\"}");

    SubscriptionConfigPayload payload = configFacade.getSubscriptionConfig(code);

    assertThat(payload.format()).isEqualTo(SubscriptionFormat.JSON);
    // Должен быть массив с двумя элементами
    assertThat(payload.body()).contains("\"format\":\"json\"");
    assertThat(payload.body()).startsWith("[");
    assertThat(payload.body()).endsWith("]");

    // Проверяем, что это валидный JSON
    Object[] parsed = new ObjectMapper().readValue(payload.body(), Object[].class);
    assertThat(parsed).hasSize(2);
  }

  @Test
  @DisplayName("Должен выбросить NotFoundException, если ни одного конфига не получено")
  void shouldThrowWhenNoConfigsResolved() {
    UUID code = UUID.randomUUID();
    User user = user(200L, "petrov");
    Server server1 = server(1L, "Moscow-1");
    Server server2 = server(2L, "SPB-1");
    Subscription subscription = activeSubscription(user, Set.of(server1, server2));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));
    when(xuiPanelService.getVlessConfig(server1, user))
        .thenThrow(new XuiUnavailableException("xui down"));
    when(xuiPanelService.getVlessConfig(server2, user))
        .thenThrow(new NotFoundException("config missing"));

    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(NotFoundException.class)
        .satisfies(error -> {
          NotFoundException ex = (NotFoundException) error;
          assertThat(ex.getEntityName()).isEqualTo("No configs found for user");
          assertThat(ex.getIdentifier()).isNull();
        });
  }

  @Test
  @DisplayName("Должен выбросить NotFoundException, если у пользователя нет подписки")
  void shouldThrowWhenSubscriptionNotFound() {
    UUID code = UUID.randomUUID();
    User user = user(300L, "sergeev");

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Subscription for user");
  }

  @Test
  @DisplayName("Должен выбросить BadRequestException, если подписка неактивна или истекла")
  void shouldThrowWhenSubscriptionIsNotActiveOrExpired() {
    UUID code = UUID.randomUUID();
    User user = user(400L, "alexeev");
    Server activeServer = server(7L, "EU-1");

    Subscription expired = new Subscription();
    expired.setUser(user);
    expired.setStatus(SubscriptionStatus.CANCELED);
    expired.setEndDate(OffsetDateTime.now().minusDays(1));
    Tariff tariff = new Tariff();
    tariff.setServers(Set.of(activeServer));
    expired.setTariff(tariff);

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Subscription is not active or expired");
  }

  @Test
  @DisplayName("Должен быстро отклонять уже исчерпанную подписку без повторной синхронизации")
  void shouldRejectExceededSubscriptionUsingCachedTrafficWithoutResync() {
    UUID code = UUID.randomUUID();
    User user = user(800L, "cached-user");
    Server server = server(21L, "CACHE-1");
    Subscription subscription = activeSubscription(user, Set.of(server));
    subscription.setId(900L);

    SubscriptionTrafficState exhausted = new SubscriptionTrafficState();
    exhausted.setUsedBytes(100L * 1024L * 1024L * 1024L);

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));
    when(trafficStateService.getTrafficStateBySubscriptionId(subscription.getId()))
        .thenReturn(Optional.of(exhausted));

    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Traffic limit exceeded");

    verify(trafficStateService, never()).syncTrafficForSubscription(any(User.class), any(Subscription.class));
    verifyNoInteractions(xuiPanelService);
  }

  @Test
  @DisplayName("Должен выбросить BadRequestException, если форматы серверов смешаны")
  void shouldThrowWhenServerFormatsAreMixed() {
    UUID code = UUID.randomUUID();
    User user = user(600L, "mixed-user");
    Server jsonServer = server(1L, "JSON-1");
    jsonServer.setSubscriptionFormat(SubscriptionFormat.JSON);
    Server vlessServer = server(2L, "VLESS-1");
    vlessServer.setSubscriptionFormat(SubscriptionFormat.VLESS);
    Subscription subscription = activeSubscription(user, Set.of(jsonServer, vlessServer));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));
    when(xuiPanelService.getJsonConfig(jsonServer, user)).thenReturn("{\"format\":\"json\"}");
    when(xuiPanelService.getVlessConfig(vlessServer, user)).thenReturn("vless://ok");

    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Mixed subscription formats");
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
    tariff.setTrafficLimitGb(100);  // Set default traffic limit

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setEndDate(OffsetDateTime.now().plusDays(30));
    subscription.setTariff(tariff);
    return subscription;
  }
}

