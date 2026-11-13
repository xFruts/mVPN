package ru.maxow.mvpn.xui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.SubscriptionFormat;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.xui.dto.XuiClientDto;

/**
 * Опциональный живой E2E тест для валидации взаимодействия с реальной 3X-UI панелью.
 * <p>
 * Активируется только при наличии переменной окружения {@code XUI_LIVE_TEST_HOST}.
 * Создаёт тестового клиента "test_testovich", проверяет создание, обновление,
 * генерацию VLESS и сбор метрик, после чего удаляет клиента.
 */
@Tag("live-xui")
@EnabledIfEnvironmentVariable(named = "XUI_LIVE_TEST_HOST", matches = ".+")
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("XuiLivePanelIT - Live 3X-UI Panel E2E Integration Tests")
class XuiLivePanelIT {

  private static final String DEFAULT_CLIENT_EMAIL = "test_testovich";

  @Autowired
  private XuiPanelService xuiPanelService;

  @Autowired
  private XuiClientApiClient clientApiClient;

  @Autowired
  private XuiSessionClient sessionClient;

  private Server liveServer;
  private User testUser;
  private Tariff testTariff;
  private Subscription testSubscription;
  private String clientEmail;

  @BeforeEach
  void initEnvironment() {
    String host = System.getenv().getOrDefault("XUI_LIVE_TEST_HOST", "");
    int port = Integer.parseInt(
        System.getenv().getOrDefault("XUI_LIVE_TEST_PORT", "2053"));
    String login = System.getenv().getOrDefault("XUI_LIVE_TEST_LOGIN", "admin");
    String password = System.getenv().getOrDefault("XUI_LIVE_TEST_PASSWORD", "");
    String authToken = System.getenv().getOrDefault(
        "XUI_LIVE_TEST_AUTH_TOKEN", "");
    String webBasePath = System.getenv().getOrDefault(
        "XUI_LIVE_TEST_WEB_BASE_PATH", "");
    clientEmail = System.getenv().getOrDefault(
        "XUI_LIVE_TEST_CLIENT_EMAIL", DEFAULT_CLIENT_EMAIL);

    liveServer = new Server();
    liveServer.setId(999L);
    liveServer.setName("Live-Test-Panel");
    liveServer.setIp(host);
    liveServer.setPort(port);
    liveServer.setXuiLogin(login);
    liveServer.setXuiPassword(password);
    liveServer.setXuiAuthToken(authToken);
    liveServer.setWebBasePath(webBasePath);
    liveServer.setStatus(ServerStatus.ACTIVE);
    liveServer.setSubscriptionFormat(SubscriptionFormat.VLESS);
    liveServer.setCountryEmoji("🧪");

    testUser = new User();
    testUser.setId(999L);
    testUser.setFullName(clientEmail);
    testUser.setXuiId(UUID.nameUUIDFromBytes(clientEmail.getBytes()));
    testUser.setXuiSubscription(UUID.randomUUID());

    testTariff = new Tariff();
    testTariff.setId(999L);
    testTariff.setName("Live Test Tariff");
    testTariff.setTrafficLimitGb(50);
    testTariff.setDurationOfDays(30);
    testTariff.setMaxDevices(2);
    testTariff.setServers(java.util.Set.of(liveServer));

    testSubscription = new Subscription();
    testSubscription.setId(999L);
    testSubscription.setUser(testUser);
    testSubscription.setTariff(testTariff);
    testSubscription.setStatus(SubscriptionStatus.ACTIVE);
    testSubscription.setStartDate(OffsetDateTime.now(ZoneOffset.UTC));
    testSubscription.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
  }

  @Test
  @Order(1)
  @DisplayName("1. createOrUpdateClient: создаёт клиента на живой панели 3X-UI")
  void shouldCreateNewClientOnLivePanel() {
    cleanUpClientQuietly();

    xuiPanelService.createOrUpdateClient(liveServer, testUser, testSubscription);

    RestClient restClient = sessionClient.buildPanelClient(liveServer);
    String sessionCookie = sessionClient.login(restClient, liveServer);
    XuiClientDto clientDto = clientApiClient.getClient(
        restClient, sessionCookie, clientEmail);

    assertThat(clientDto).isNotNull();
    assertThat(clientDto.email()).isEqualTo(clientEmail);
    assertThat(clientDto.enable()).isTrue();
    assertThat(clientDto.totalGB()).isEqualTo(testTariff.getTrafficLimitBytes());
  }

  @Test
  @Order(2)
  @DisplayName("2. createOrUpdateClient: успешно обновляет клиента без ошибок валидации")
  void shouldUpdateExistingClientOnLivePanel() {
    testTariff.setTrafficLimitGb(100);
    testTariff.setMaxDevices(5);
    testSubscription.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(60));

    xuiPanelService.createOrUpdateClient(liveServer, testUser, testSubscription);

    RestClient restClient = sessionClient.buildPanelClient(liveServer);
    String sessionCookie = sessionClient.login(restClient, liveServer);
    XuiClientDto clientDto = clientApiClient.getClient(
        restClient, sessionCookie, clientEmail);

    assertThat(clientDto).isNotNull();
    assertThat(clientDto.email()).isEqualTo(clientEmail);
    assertThat(clientDto.totalGB()).isEqualTo(testTariff.getTrafficLimitBytes());
    assertThat(clientDto.limitIp()).isEqualTo(5);
  }

  @Test
  @Order(3)
  @DisplayName("3. getVlessConfig: формирует рабочую VLESS-ссылку для клиента")
  void shouldGenerateVlessConfigFromLivePanel() {
    String vlessLink = xuiPanelService.getVlessConfig(liveServer, testUser);

    assertThat(vlessLink).isNotNull();
    assertThat(vlessLink).startsWith("vless://");
    assertThat(vlessLink).contains(String.valueOf(testUser.getXuiId()));
    assertThat(vlessLink).contains(liveServer.getIp());
  }

  @Test
  @Order(4)
  @DisplayName("4. getClientTraffic: считывает статистику трафика клиента")
  void shouldFetchClientTrafficFromLivePanel() {
    XuiClientTraffic traffic = xuiPanelService.getClientTraffic(
        liveServer, clientEmail);

    assertThat(traffic).isNotNull();
    assertThat(traffic.email()).isEqualTo(clientEmail);
    assertThat(traffic.upload()).isGreaterThanOrEqualTo(0L);
    assertThat(traffic.download()).isGreaterThanOrEqualTo(0L);
  }

  @AfterAll
  void tearDown() {
    cleanUpClientQuietly();
  }

  private void cleanUpClientQuietly() {
    if (liveServer == null || clientEmail == null) {
      return;
    }
    try {
      RestClient restClient = sessionClient.buildPanelClient(liveServer);
      String sessionCookie = sessionClient.login(restClient, liveServer);
      clientApiClient.deleteClient(
          restClient, sessionCookie, clientEmail, liveServer);
    } catch (Exception ignored) {
      // Игнорируем ошибки при очистке
    }
  }
}
