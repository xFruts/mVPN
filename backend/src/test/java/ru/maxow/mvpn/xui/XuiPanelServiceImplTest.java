package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.web.client.HttpClientErrorException;
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
  private RestClient.RequestBodySpec settingsBodySpec;
  private RestClient.RequestBodySpec addClientBodySpec;
  private RestClient.RequestBodySpec updateClientBodySpec;
  private RestClient.ResponseSpec loginResponseSpec;
  private RestClient.ResponseSpec settingsResponseSpec;
  private RestClient.ResponseSpec inboundsResponseSpec;
  private RestClient.ResponseSpec jsonResponseSpec;
  private RestClient.ResponseSpec trafficResponseSpec;
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

    mockLoginSuccess(2);
    mockUpdateClientSuccess();
    mockInboundsSequence(response, response);
    mockSubPortResponse(2096);
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId()))
        .thenReturn(activeSubscription());

    String config = service.getVlessConfig(server, user);

    assertThat(config).contains("vless://" + user.getXuiId() + "@1.2.3.4:443?");
    assertThat(config).contains("#moscow");
    verify(subscriptionService).findLastSubscriptionEntityByUserId(user.getId());
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

    mockLoginSuccess(2);
    mockUpdateClientSuccess();
    mockInboundsSequence(disabledByEmail, enabledByXuiId);
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId()))
        .thenReturn(activeSubscription());

    String config = service.getVlessConfig(server, user);

    assertThat(config).contains("vless://" + user.getXuiId() + "@");
    verify(postUriSpec).uri("/panel/api/inbounds/updateClient/" + otherId);
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

    mockLoginSuccess(2);
    mockAddClientSuccess();
    mockInboundsSequence(withoutClient, withClient);
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

    mockLoginSuccess(2);
    mockAddClientSuccess();
    mockInboundsSequence(withoutClient, withoutClient);
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

    mockLoginSuccess(2);
    mockUpdateClientSuccess();
    mockInboundsSequence(response, response);
    mockSubPortResponse(2096);
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId()))
        .thenReturn(activeSubscription());

    String config = service.getVlessConfig(server, user);

    assertThat(config).contains("vless://" + user.getXuiId() + "@1.2.3.4:8443?");
    assertThat(config).contains("type=ws");
    assertThat(config).contains("security=tls");
    assertThat(config).contains("fp=chrome");
    assertThat(config).contains("sni=site.example");
    assertThat(config).contains("path=%2Fws");
    assertThat(config).contains("host=edge.example");
  }

  @Test
  @DisplayName("Возвращает JSON конфиг после базовой подготовки клиента")
  void shouldReturnJsonConfigAfterClientPreparation() {
    Server server = testServer();
    User user = testUser("json-user");

    XuiInboundsResponse response = inboundsResponse(List.of(
        inbound(1, 443, "moscow", user.getXuiId().toString(), "json-user", true, 0)
    ));

    mockLoginSuccess(2);
    mockUpdateClientSuccess();
    mockInboundsSequence(response, response);
    mockSubPortResponse(2096);
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId()))
        .thenReturn(activeSubscription());
    when(jsonResponseSpec.body(String.class)).thenReturn("{\"format\":\"json\"}");

    String config = service.getJsonConfig(server, user);

    assertThat(config).isEqualTo("{\"format\":\"json\",\"remarks\":\"Moscow-1\"}");
  }

  @Test
  @DisplayName("Бросает XuiUnavailableException, если JSON endpoint вернул Error!")
  void shouldThrowWhenJsonEndpointReturnsErrorText() {
    Server server = testServer();
    User user = testUser("json-user");

    XuiInboundsResponse response = inboundsResponse(List.of(
        inbound(1, 443, "moscow", user.getXuiId().toString(), "json-user", true, 0)
    ));

    mockLoginSuccess(2);
    mockUpdateClientSuccess();
    mockInboundsSequence(response, response);
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId()))
        .thenReturn(activeSubscription());
    when(jsonResponseSpec.body(String.class)).thenReturn("Error!");

    assertThatThrownBy(() -> service.getJsonConfig(server, user))
        .isInstanceOf(XuiUnavailableException.class)
        .hasMessageContaining("invalid JSON subscription response");
  }

  @Test
  @DisplayName("Бросает XuiUnavailableException, если /json/{subId} вернул 404")
  void shouldThrowWhenRootJsonPathReturns404() {
    Server server = testServer();
    User user = testUser("json-user");

    XuiInboundsResponse response = inboundsResponse(List.of(
        inbound(1, 443, "moscow", user.getXuiId().toString(), "json-user", true, 0)
    ));

    mockLoginSuccess(2);
    mockUpdateClientSuccess();
    mockInboundsSequence(response, response);
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId()))
        .thenReturn(activeSubscription());
    when(jsonResponseSpec.body(String.class))
        .thenThrow(HttpClientErrorException.create(
            HttpStatus.NOT_FOUND,
            "Not Found",
            HttpHeaders.EMPTY,
            new byte[0],
            null));

    assertThatThrownBy(() -> service.getJsonConfig(server, user))
        .isInstanceOf(XuiUnavailableException.class)
        .hasMessageContaining("Failed to get JSON config from XUI server");
  }

  @Test
  @DisplayName("Использует fallback subPort=2096, если /panel/setting/all недоступен")
  void shouldFallbackToDefaultSubPortWhenSettingsRequestFails() {
    Server server = testServer();
    User user = testUser("json-user");

    XuiInboundsResponse response = inboundsResponse(List.of(
        inbound(1, 443, "moscow", user.getXuiId().toString(), "json-user", true, 0)
    ));

    mockLoginSuccess(2);
    mockUpdateClientSuccess();
    mockInboundsSequence(response, response);
    when(settingsResponseSpec.body(eq(JsonNode.class))).thenThrow(new RuntimeException("settings down"));
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId()))
        .thenReturn(activeSubscription());
    when(jsonResponseSpec.body(String.class)).thenReturn("{\"format\":\"json\"}");

    String config = service.getJsonConfig(server, user);

    assertThat(config).isEqualTo("{\"format\":\"json\",\"remarks\":\"Moscow-1\"}");
    verify(postUriSpec).uri("/panel/setting/all");
  }

  @Test
  @DisplayName("Корректно парсит traffic-ответ XUI с лишним полем uuid")
  void shouldParseTrafficResponseWithUuidField() {
    Server server = testServer();
    String clientId = UUID.randomUUID().toString();

    mockLoginSuccess(1);
    String responseBody = """
        {"success":true,"msg":"","obj":[{"id":28,"inboundId":1,"enable":true,"email":"Veles","uuid":"fb5e3a84-0b0a-4b9f-a473-1fc4247c98b4","subId":"900ba993-f787-4e28-9ca5-0bee9b183a4f","up":17893514,"down":70659736,"allTime":88553250,"expiryTime":1779987036226,"total":107374182400,"reset":0,"lastOnline":1777395194027}]}
        """;
    when(trafficResponseSpec.body(String.class)).thenReturn(responseBody);

    XuiClientTraffic traffic = service.getClientTraffic(server, clientId);

    assertThat(traffic.getUpload()).isEqualTo(17893514L);
    assertThat(traffic.getDownload()).isEqualTo(70659736L);
    assertThat(traffic.getTotal()).isEqualTo(107374182400L);
    assertThat(traffic.getEmail()).isEqualTo("Veles");
    assertThat(traffic.getEnable()).isTrue();
  }

  @Test
  @DisplayName("Возвращает нулевой traffic, если XUI вернул пустой obj")
  void shouldReturnEmptyTrafficWhenObjIsEmpty() {
    Server server = testServer();
    String clientId = UUID.randomUUID().toString();

    mockLoginSuccess(1);
    when(trafficResponseSpec.body(String.class)).thenReturn("{\"success\":true,\"msg\":\"\",\"obj\":[]}");

    XuiClientTraffic traffic = service.getClientTraffic(server, clientId);

    assertThat(traffic.getUpload()).isEqualTo(0L);
    assertThat(traffic.getDownload()).isEqualTo(0L);
    assertThat(traffic.getAllTime()).isEqualTo(0L);
    assertThat(traffic.getTotal()).isEqualTo(0L);
    assertThat(traffic.getEnable()).isFalse();
  }

  private void configurePostChain() {
    postUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    loginBodySpec = mock(RestClient.RequestBodySpec.class);
    settingsBodySpec = mock(RestClient.RequestBodySpec.class);
    addClientBodySpec = mock(RestClient.RequestBodySpec.class);
    updateClientBodySpec = mock(RestClient.RequestBodySpec.class);
    loginResponseSpec = mock(RestClient.ResponseSpec.class);
    settingsResponseSpec = mock(RestClient.ResponseSpec.class);
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
      if ("/panel/setting/all".equals(uri)) {
        return settingsBodySpec;
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

    when(settingsBodySpec.header(eq(HttpHeaders.COOKIE), any(String[].class))).thenReturn(settingsBodySpec);
    when(settingsBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(settingsBodySpec);
    when(settingsBodySpec.body(any(MultiValueMap.class))).thenReturn(settingsBodySpec);
    when(settingsBodySpec.retrieve()).thenReturn(settingsResponseSpec);

    when(updateClientBodySpec.header(eq(HttpHeaders.COOKIE), any(String[].class))).thenReturn(updateClientBodySpec);
    when(updateClientBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(updateClientBodySpec);
    when(updateClientBodySpec.body(any(MultiValueMap.class))).thenReturn(updateClientBodySpec);
    when(updateClientBodySpec.retrieve()).thenReturn(updateClientResponseSpec);
  }

  private void configureGetInboundsChain() {
    RestClient.RequestHeadersUriSpec<?> getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.RequestHeadersSpec<?> inboundsHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    RestClient.RequestHeadersSpec<?> jsonHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    RestClient.RequestHeadersSpec<?> trafficHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    inboundsResponseSpec = mock(RestClient.ResponseSpec.class);
    jsonResponseSpec = mock(RestClient.ResponseSpec.class);
    trafficResponseSpec = mock(RestClient.ResponseSpec.class);

    doReturn(getUriSpec).when(restClient).get();
    when(getUriSpec.uri(anyString())).thenAnswer(invocation -> {
      String uri = invocation.getArgument(0, String.class);
      if ("/panel/api/inbounds/list".equals(uri)) {
        return inboundsHeadersSpec;
      }
      if (uri.contains("/json/")) {
        return jsonHeadersSpec;
      }
      if (uri.contains("/panel/api/inbounds/getClientTrafficsById/")) {
        return trafficHeadersSpec;
      }
      throw new IllegalStateException("Unexpected GET URI: " + uri);
    });
    doReturn(inboundsHeadersSpec).when(inboundsHeadersSpec)
        .header(eq(HttpHeaders.COOKIE), any(String[].class));
    doReturn(inboundsResponseSpec).when(inboundsHeadersSpec).retrieve();
    doReturn(jsonHeadersSpec).when(jsonHeadersSpec).header(eq(HttpHeaders.COOKIE), any(String[].class));
    doReturn(jsonResponseSpec).when(jsonHeadersSpec).retrieve();
    doReturn(trafficHeadersSpec).when(trafficHeadersSpec).header(eq(HttpHeaders.COOKIE), any(String[].class));
    doReturn(trafficResponseSpec).when(trafficHeadersSpec).retrieve();
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

  private void mockSubPortResponse(int subPort) {
    var mapper = new ObjectMapper();
    JsonNode response = mapper.createObjectNode()
        .set("obj", mapper.createObjectNode().put("subPort", subPort));
    when(settingsResponseSpec.body(eq(JsonNode.class))).thenReturn(response);
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

