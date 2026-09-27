package ru.maxow.mvpn.xui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.maxow.mvpn.minio.MinioService;
import ru.maxow.mvpn.model.BroadcastRequestDto;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.ServerRepository;
import ru.maxow.mvpn.server.SubscriptionFormat;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficStateService;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.tariff.TariffRepository;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.xui.config.ConfigFacade;
import ru.maxow.mvpn.xui.dto.SubscriptionConfigPayload;

@SpringBootTest(properties = {
    "spring.datasource.hikari.maximum-pool-size=5",
    "spring.datasource.hikari.connection-timeout=3000",
    "spring.datasource.hikari.leak-detection-threshold=2000"
})
@ActiveProfiles("test")
class ConfigFacadeConcurrencyLoadTest {

  @Autowired
  private ConfigFacade configFacade;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ServerRepository serverRepository;

  @Autowired
  private TariffRepository tariffRepository;

  @Autowired
  private SubscriptionRepository subscriptionRepository;

  @MockitoBean
  private MinioService minioService;

  @MockitoBean
  private XuiPanelService xuiPanelService;

  @MockitoBean
  private SubscriptionTrafficStateService trafficStateService;

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private KafkaTemplate<String, BroadcastRequestDto> kafkaTemplate;

  private UUID verificationCode;

  @BeforeEach
  void setUp() {
    subscriptionRepository.deleteAll();
    tariffRepository.deleteAll();
    serverRepository.deleteAll();
    userRepository.deleteAll();

    User user = new User();
    user.setFullName("LoadTestUser_" + UUID.randomUUID());
    user.setRole(UserRole.REGULAR);
    user.setVerificationCode(UUID.randomUUID());
    user = userRepository.save(user);
    this.verificationCode = user.getVerificationCode();

    Server server = new Server();
    server.setName("LoadTestServer");
    server.setIp("10.0.0.1");
    server.setStatus(ServerStatus.ACTIVE);
    server.setSubscriptionFormat(SubscriptionFormat.VLESS);
    server = serverRepository.save(server);

    Tariff tariff = new Tariff();
    tariff.setName("LoadTestTariff_" + UUID.randomUUID());
    tariff.setTrafficLimitGb(50);
    tariff.setDurationOfDays(30);
    tariff.setMaxDevices(3);
    tariff.setServers(Set.of(server));
    tariff = tariffRepository.save(tariff);

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setTariff(tariff);
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setStartDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
    subscription.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(29));
    subscriptionRepository.save(subscription);

    SubscriptionTrafficState trafficState = new SubscriptionTrafficState();
    trafficState.setUsedBytes(1024L);
    trafficState.setUsedUploadBytes(512L);
    trafficState.setUsedDownloadBytes(512L);

    when(trafficStateService.getTrafficStateBySubscriptionId(any())).thenReturn(Optional.of(trafficState));
    when(trafficStateService.syncTrafficForSubscription(any(), any())).thenReturn(trafficState);

    // Simulate external network latency to XUI panel (50ms)
    when(xuiPanelService.getVlessConfig(any(), any())).thenAnswer(invocation -> {
      Thread.sleep(50);
      return "vless://test-uuid@10.0.0.1:443?security=reality#TestServer";
    });
  }

  @Test
  @DisplayName("Under 30 concurrent requests with only 5 DB connections and 50ms network delay, no connection starvation occurs")
  void shouldHandleConcurrentRequestsWithoutConnectionPoolStarvation() throws Exception {
    int concurrentThreads = 30;
    ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(concurrentThreads);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);
    List<Throwable> exceptions = new ArrayList<>();

    List<Callable<Void>> tasks = new ArrayList<>();
    for (int i = 0; i < concurrentThreads; i++) {
      tasks.add(() -> {
        try {
          startLatch.await(); // Ensure all threads start at the exact same instant
          SubscriptionConfigPayload payload = configFacade.getSubscriptionConfig(verificationCode);
          if (payload != null && payload.body() != null) {
            successCount.incrementAndGet();
          } else {
            failureCount.incrementAndGet();
          }
        } catch (Throwable t) {
          synchronized (exceptions) {
            exceptions.add(t);
          }
          failureCount.incrementAndGet();
        } finally {
          doneLatch.countDown();
        }
        return null;
      });
    }

    List<Future<Void>> futures = new ArrayList<>();
    for (Callable<Void> task : tasks) {
      futures.add(executor.submit(task));
    }

    // Unleash all concurrent threads simultaneously
    startLatch.countDown();

    // Background queries simulating healthcheck / monitoring queries during peak traffic
    for (int i = 0; i < 5; i++) {
      Thread.sleep(20);
      List<Server> servers = serverRepository.findAll();
      assertThat(servers).isNotEmpty();
    }

    boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
    executor.shutdown();

    assertThat(completed).isTrue();
    assertThat(failureCount.get())
        .withFailMessage("Expected 0 failures but got %d. Exceptions: %s", failureCount.get(), exceptions)
        .isEqualTo(0);
    assertThat(successCount.get()).isEqualTo(concurrentThreads);
  }
}
