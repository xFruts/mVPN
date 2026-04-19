package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("XuiPanelServiceImpl - Unit тесты")
class XuiPanelServiceImplTest {

  @Mock
  private RestClient.Builder restClientBuilder;
  @Mock
  private RestClient restClient;
  @Mock
  private SubscriptionService subscriptionService;

  private XuiPanelServiceImpl service;

  private RestClient.RequestBodyUriSpec postUriSpec;
  private RestClient.RequestBodySpec loginBodySpec;
  private RestClient.RequestBodySpec addClientBodySpec;
  private RestClient.RequestBodySpec updateClientBodySpec;
  private RestClient.ResponseSpec loginResponseSpec;
  private RestClient.ResponseSpec inboundsResponseSpec;
  private RestClient.ResponseSpec addClientResponseSpec;
  private RestClient.ResponseSpec updateClientResponseSpec;

  @BeforeEach
  void setUp() {
    service = new XuiPanelServiceImpl(restClientBuilder, new ObjectMapper(), subscriptionService);

    when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
    when(restClientBuilder.build()).thenReturn(restClient);

    configurePostChain();
    configureGetInboundsChain();
  }

  @Test
  @DisplayName("Возвращает конфиг, когда клиент найден по xuiId")
  void shouldReturnConfigWhenClientFoundByXuiId() {
    Server server = testServer();
    User user = testUser("new-name");

    XuiInboundsResponse response = inboundsResponse(List.of(
        inbound(1, 443, "moscow", user.getXuiId().toString(), "old-name", true, 0)
    ));

    mockLoginSuccess(1);
    mockInboundsSequence(response);

    String config = service.getVlessConfig(server, user);

    assertThat(config).contains("vless://" + user.getXuiId() + "@1.2.3.4:443?");
    assertThat(config).contains("#moscow");
    verify(subscriptionService, never()).findLastSubscriptionEntityByUserId(any());
  }

  @Test
  @DisplayName("Делает updateClient при fallback по email и возвращает конфиг после retry")
  void shouldUpdateClientWhenFoundByEmailFallback() {
    Server server = testServer();
    User user = testUser("petrov");

    String otherId = UUID.randomUUID().toString();
    XuiInboundsResponse disabledByEmail = inboundsResponse(List.of(
        inbound(1, 443, "first", otherId, "petrov", false, 0)
    ));
    XuiInboundsResponse enabledByXuiId = inboundsResponse(List.of(
        inbound(1, 443, "first", user.getXuiId().toString(), "petrov", true, 0)
    ));

    mockLoginSuccess(3);
    mockUpdateClientSuccess();
    mockInboundsSequence(disabledByEmail, disabledByEmail, enabledByXuiId);
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId()))
        .thenReturn(activeSubscription());

    String config = service.getVlessConfig(server, user);

    assertThat(config).contains("vless://" + user.getXuiId() + "@");
    verify(postUriSpec).uri("/panel/api/inbounds/updateClient/" + user.getXuiId());
    verify(subscriptionService).findLastSubscriptionEntityByUserId(user.getId());
  }

  @Test
  @DisplayName("Делает addClient, когда клиента нет, и возвращает конфиг после retry")
  void shouldAddClientWhenMissingAndRetry() {
    Server server = testServer();
    User user = testUser("sidorov");

    XuiInboundsResponse withoutClient = inboundsResponse(List.of(
        inbound(1, 443, "first", UUID.randomUUID().toString(), "other-user", true, 0)
    ));
    XuiInboundsResponse withClient = inboundsResponse(List.of(
        inbound(1, 443, "first", user.getXuiId().toString(), "sidorov", true, 0)
    ));

    mockLoginSuccess(3);
    mockAddClientSuccess();
    mockInboundsSequence(withoutClient, withoutClient, withClient);
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId()))
        .thenReturn(activeSubscription());

    String config = service.getVlessConfig(server, user);

    assertThat(config).contains("vless://" + user.getXuiId() + "@");
    verify(postUriSpec).uri("/panel/api/inbounds/addClient");
    verify(subscriptionService).findLastSubscriptionEntityByUserId(user.getId());
  }

  @Test
  @DisplayName("Бросает NotFound после retry, если конфиг так и не найден")
  void shouldThrowNotFoundAfterRetryWhenConfigStillMissing() {
    Server server = testServer();
    User user = testUser("no-user");

    XuiInboundsResponse withoutClient = inboundsResponse(List.of(
        inbound(1, 443, "first", UUID.randomUUID().toString(), "other-user", true, 0)
    ));

    mockLoginSuccess(3);
    mockAddClientSuccess();
    mockInboundsSequence(withoutClient, withoutClient, withoutClient);
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId()))
        .thenReturn(activeSubscription());

    assertThatThrownBy(() -> service.getVlessConfig(server, user))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Config for user");
  }

  @Test
  @DisplayName("Бросает XuiUnavailableException, если на сервере нет VLESS inbound")
  void shouldThrowWhenNoVlessInboundFound() {
    Server server = testServer();
    User user = testUser("ivanov");

    XuiInboundsResponse noVless = inboundsResponse(List.of(inboundWithProtocol("trojan")));

    mockLoginSuccess(1);
    mockInboundsSequence(noVless);

    assertThatThrownBy(() -> service.createClient(server, user))
        .isInstanceOf(XuiUnavailableException.class)
        .hasMessageContaining("No VLESS inbound found");
  }

  @Test
  @DisplayName("Бросает XuiUnavailableException, если login не вернул cookie")
  void shouldThrowWhenLoginCookieMissing() {
    Server server = testServer();
    User user = testUser("ivanov");

    when(loginResponseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

    assertThatThrownBy(() -> service.getVlessConfig(server, user))
        .isInstanceOf(XuiUnavailableException.class)
        .hasMessageContaining("No session cookie");
  }

  @Test
  @DisplayName("Формирует ссылку с TLS/WS параметрами")
  void shouldGenerateTlsWsLinkWithExpectedParams() {
    Server server = testServer();
    User user = testUser("ws-user");

    XuiInboundsResponse response = inboundsResponse(List.of(tlsWsInbound(user.getXuiId().toString(), "ws-user")));

    mockLoginSuccess(1);
    mockInboundsSequence(response);

    String config = service.getVlessConfig(server, user);

    assertThat(config).contains("vless://" + user.getXuiId() + "@1.2.3.4:8443?");
    assertThat(config).contains("type=ws");
    assertThat(config).contains("security=tls");
    assertThat(config).contains("fp=chrome");
    assertThat(config).contains("sni=site.example");
    assertThat(config).contains("path=%2Fws");
    assertThat(config).contains("host=edge.example");
  }

  private void configurePostChain() {
    postUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    loginBodySpec = mock(RestClient.RequestBodySpec.class);
    addClientBodySpec = mock(RestClient.RequestBodySpec.class);
    updateClientBodySpec = mock(RestClient.RequestBodySpec.class);
    loginResponseSpec = mock(RestClient.ResponseSpec.class);
    addClientResponseSpec = mock(RestClient.ResponseSpec.class);
    updateClientResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.post()).thenReturn(postUriSpec);
    when(postUriSpec.uri(anyString())).thenAnswer(invocation -> {
      String uri = invocation.getArgument(0, String.class);
      if ("/login".equals(uri)) {
        return loginBodySpec;
      }
      if ("/panel/api/inbounds/addClient".equals(uri)) {
        return addClientBodySpec;
      }
      if (uri.startsWith("/panel/api/inbounds/updateClient/")) {
        return updateClientBodySpec;
      }
      throw new IllegalStateException("Unexpected URI: " + uri);
    });

    when(loginBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(loginBodySpec);
    when(loginBodySpec.body(any(MultiValueMap.class))).thenReturn(loginBodySpec);
    when(loginBodySpec.retrieve()).thenReturn(loginResponseSpec);

    when(addClientBodySpec.header(eq(HttpHeaders.COOKIE), any(String[].class))).thenReturn(addClientBodySpec);
    when(addClientBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(addClientBodySpec);
    when(addClientBodySpec.body(any(MultiValueMap.class))).thenReturn(addClientBodySpec);
    when(addClientBodySpec.retrieve()).thenReturn(addClientResponseSpec);

    when(updateClientBodySpec.header(eq(HttpHeaders.COOKIE), any(String[].class))).thenReturn(updateClientBodySpec);
    when(updateClientBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(updateClientBodySpec);
    when(updateClientBodySpec.body(any(MultiValueMap.class))).thenReturn(updateClientBodySpec);
    when(updateClientBodySpec.retrieve()).thenReturn(updateClientResponseSpec);
  }

  private void configureGetInboundsChain() {
    RestClient.RequestHeadersUriSpec<?> getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.RequestHeadersSpec<?> getHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    inboundsResponseSpec = mock(RestClient.ResponseSpec.class);

    doReturn(getUriSpec).when(restClient).get();
    doReturn(getHeadersSpec).when(getUriSpec).uri("/panel/api/inbounds/list");
    doReturn(getHeadersSpec).when(getHeadersSpec).header(eq(HttpHeaders.COOKIE), any(String[].class));
    doReturn(inboundsResponseSpec).when(getHeadersSpec).retrieve();
  }

  private void mockLoginSuccess(int calls) {
    ResponseEntity<Void> response = loginResponseWithCookie();
    var stubbing = when(loginResponseSpec.toBodilessEntity()).thenReturn(response);
    for (int i = 1; i < calls; i++) {
      stubbing = stubbing.thenReturn(response);
    }
  }

  private void mockInboundsSequence(XuiInboundsResponse... responses) {
    if (responses.length == 1) {
      when(inboundsResponseSpec.body(XuiInboundsResponse.class)).thenReturn(responses[0]);
      return;
    }
    when(inboundsResponseSpec.body(XuiInboundsResponse.class))
        .thenReturn(responses[0], Arrays.copyOfRange(responses, 1, responses.length));
  }

  private void mockAddClientSuccess() {
    when(addClientResponseSpec.body(String.class)).thenReturn("ok");
  }

  private void mockUpdateClientSuccess() {
    when(updateClientResponseSpec.body(String.class)).thenReturn("ok");
  }

  private ResponseEntity<Void> loginResponseWithCookie() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, "session=ok");
    return new ResponseEntity<>(headers, HttpStatus.OK);
  }

  private Server testServer() {
    Server server = new Server();
    server.setId(10L);
    server.setName("Moscow-1");
    server.setIp("1.2.3.4");
    server.setPort(443);
    server.setXuiLogin("xui");
    server.setXuiPassword("xui-pass");
    server.setWebBasePath("");
    server.setCountryEmoji("");
    return server;
  }

  private User testUser(String fullName) {
    User user = new User();
    user.setId(101L);
    user.setFullName(fullName);
    user.setXuiId(UUID.randomUUID());
    user.setXuiSubscription(UUID.randomUUID());
    user.setUserTelegramId(12345L);
    return user;
  }

  private Subscription activeSubscription() {
    Tariff tariff = new Tariff();
    tariff.setMaxDevices(5);
    tariff.setTrafficLimitGb(200);

    Subscription subscription = new Subscription();
    subscription.setTariff(tariff);
    subscription.setEndDate(OffsetDateTime.now().plusDays(30));
    return subscription;
  }

  private XuiInboundsResponse inboundsResponse(List<XuiInboundsResponse.Inbound> inbounds) {
    XuiInboundsResponse response = new XuiInboundsResponse();
    response.setSuccess(true);
    response.setObj(inbounds);
    return response;
  }

  private XuiInboundsResponse.Inbound inbound(
      int id,
      int port,
      String remark,
      String clientId,
      String email,
      boolean enabled,
      long expiryTimeMillis) {
    XuiInboundsResponse.Inbound inbound = new XuiInboundsResponse.Inbound();
    inbound.setId(id);
    inbound.setPort(port);
    inbound.setProtocol("vless");
    inbound.setTag("tag-" + id);
    inbound.setRemark(remark);
    inbound.setSettings("""
        {"clients":[{"id":"%s","email":"%s","enable":%s,"flow":"xtls-rprx-vision","expiryTime":%d}]}
        """.formatted(clientId, email, enabled, expiryTimeMillis));
    inbound.setStreamSettings("""
        {"network":"grpc","security":"reality","realitySettings":{"settings":{"publicKey":"pk","fingerprint":"chrome","spiderX":"/"},"serverNames":["sni.example"],"shortIds":["ab12"]},"grpcSettings":{"serviceName":"svc"}}
        """);
    return inbound;
  }

  private XuiInboundsResponse.Inbound tlsWsInbound(String clientId, String email) {
    XuiInboundsResponse.Inbound inbound = new XuiInboundsResponse.Inbound();
    inbound.setId(3);
    inbound.setPort(8443);
    inbound.setProtocol("vless");
    inbound.setTag("tag-3");
    inbound.setRemark("ws-remark");
    inbound.setSettings("""
        {"clients":[{"id":"%s","email":"%s","enable":true,"flow":"xtls-rprx-vision"}]}
        """.formatted(clientId, email));
    inbound.setStreamSettings("""
        {"network":"ws","security":"tls","tlsSettings":{"fingerprint":"chrome","serverName":"site.example"},"wsSettings":{"path":"/ws","headers":{"Host":"edge.example"}}}
        """);
    return inbound;
  }

  private XuiInboundsResponse.Inbound inboundWithProtocol(String protocol) {
    XuiInboundsResponse.Inbound inbound = new XuiInboundsResponse.Inbound();
    inbound.setId(10);
    inbound.setPort(443);
    inbound.setProtocol(protocol);
    inbound.setTag("tag-10");
    inbound.setRemark("remark");
    inbound.setSettings("{\"clients\":[]}");
    inbound.setStreamSettings("{\"network\":\"grpc\",\"security\":\"none\"}");
    return inbound;
  }
}

