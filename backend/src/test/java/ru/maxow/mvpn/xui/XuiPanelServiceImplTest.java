package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("XuiPanelServiceImpl - Unit тесты")
class XuiPanelServiceImplTest {

  @Mock private XuiSessionClient sessionClient;
  @Mock private XuiClientProvisioner clientProvisioner;
  @Mock private XuiClientTrafficReader trafficReader;
  @Mock private XuiVlessConfigService vlessConfigService;
  @Mock private XuiJsonConfigClient jsonConfigClient;
  @Mock private RestClient restClient;

  private XuiPanelServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new XuiPanelServiceImpl(
        sessionClient,
        clientProvisioner,
        trafficReader,
        vlessConfigService,
        jsonConfigClient);

    when(sessionClient.buildPanelClient(any())).thenReturn(restClient);
    when(sessionClient.buildRootClient(any())).thenReturn(restClient);
  }

  @Test
  @DisplayName("getVlessConfig: возвращает конфиг от vlessConfigService")
  void shouldReturnConfigWhenClientFound() {
    Server server = testServer();
    User user = testUser();

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    when(vlessConfigService.resolveVlessConfig(restClient, "session=ok", server, user))
        .thenReturn("vless://test-config");

    String result = service.getVlessConfig(server, user);

    assertThat(result).isEqualTo("vless://test-config");
    verify(sessionClient).login(restClient, server);
    verify(vlessConfigService).resolveVlessConfig(restClient, "session=ok", server, user);
  }

  @Test
  @DisplayName("getVlessConfig: пробрасывает NotFoundException без оборачивания")
  void shouldPropagateNotFoundException() {
    Server server = testServer();
    User user = testUser();

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    when(vlessConfigService.resolveVlessConfig(restClient, "session=ok", server, user))
        .thenThrow(new NotFoundException("User config not found"));

    assertThatThrownBy(() -> service.getVlessConfig(server, user))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  @DisplayName("Обрабатывает ResourceAccessException как XuiUnavailableException")
  void shouldWrapResourceAccessExceptionInXuiUnavailableException() {
    Server server = testServer();
    User user = testUser();

    when(sessionClient.login(eq(restClient), eq(server)))
        .thenThrow(new ResourceAccessException("Connection timed out"));

    assertThatThrownBy(() -> service.getVlessConfig(server, user))
        .isInstanceOf(XuiUnavailableException.class)
        .hasMessageContaining("Network error while connecting to XUI server");
  }

  @Test
  @DisplayName("createOrUpdateClient: успешно делегирует в clientProvisioner")
  void shouldDelegateCreateOrUpdateClient() {
    Server server = testServer();
    User user = testUser();
    Subscription subscription = testSubscription(user);

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    doNothing().when(clientProvisioner)
        .upsertClient(restClient, "session=ok", server, user, subscription);

    service.createOrUpdateClient(server, user, subscription);

    verify(clientProvisioner)
        .upsertClient(restClient, "session=ok", server, user, subscription);
  }

  @Test
  @DisplayName("getClientTraffic: успешно возвращает трафик")
  void shouldReturnClientTraffic() {
    Server server = testServer();
    XuiClientTraffic traffic = new XuiClientTraffic("test-user", 100L, 200L, 300L, 0L);

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    when(trafficReader.getClientTraffic(restClient, "session=ok", server, "test-user"))
        .thenReturn(traffic);

    XuiClientTraffic result = service.getClientTraffic(server, "test-user");

    assertThat(result).isNotNull();
    assertThat(result.total()).isEqualTo(300L);
    verify(trafficReader).getClientTraffic(restClient, "session=ok", server, "test-user");
  }

  private Server testServer() {
    Server server = new Server();
    server.setId(10L);
    server.setName("Moscow-1");
    server.setIp("1.2.3.4");
    return server;
  }

  private User testUser() {
    User user = new User();
    user.setId(101L);
    user.setFullName("test-user");
    user.setXuiId(UUID.randomUUID());
    user.setXuiSubscription(UUID.randomUUID());
    return user;
  }

  private Subscription testSubscription(User user) {
    Tariff tariff = new Tariff();
    tariff.setTrafficLimitGb(100);
    tariff.setMaxDevices(3);

    Subscription subscription = new Subscription();
    subscription.setId(1001L);
    subscription.setUser(user);
    subscription.setTariff(tariff);
    subscription.setEndDate(OffsetDateTime.now().plusMonths(1));
    return subscription;
  }
}
