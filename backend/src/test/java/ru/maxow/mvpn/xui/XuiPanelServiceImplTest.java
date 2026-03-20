package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("XuiPanelServiceImpl - Unit тесты")
class XuiPanelServiceImplTest {

  @Mock
  private RestClient.Builder restClientBuilder;

  @Mock
  private RestClient restClient;

  private XuiPanelServiceImpl service;

  private RestClient.RequestBodySpec loginBodySpec;
  private RestClient.ResponseSpec loginResponseSpec;
  private RestClient.ResponseSpec inboundsResponseSpec;
  private RestClient.ResponseSpec addClientResponseSpec;

  @BeforeEach
  void setUp() {
    service = new XuiPanelServiceImpl(restClientBuilder, new ObjectMapper());

    when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
    when(restClientBuilder.build()).thenReturn(restClient);

    configureLoginChain();
    configureInboundsChain();
    configureAddClientChain();
  }

  @Test
  @DisplayName("Должен использовать inbound, в котором найден клиент (корректный порт в ссылке)")
  void shouldUseMatchedInboundWhenGeneratingLink() {
    Server server = testServer();
    User user = testUser("ivanov");

    XuiInboundsResponse response = inboundsResponse(List.of(
        inbound(1, 443, "first", "other-user"),
        inbound(2, 8443, "second", "ivanov")
    ));

    mockLoginSuccess(1);
    mockInboundsSequence(response);

    String config = service.getVlessConfig(server, user);

    assertThat(config).contains("vless://uuid-ivanov@");
    assertThat(config).contains(":8443?");
    assertThat(config).contains("#second");
  }

  @Test
  @DisplayName("Должен создать клиента и повторить поиск, если конфиг сначала не найден")
  void shouldCreateClientAndRetryWhenConfigMissing() {
    Server server = testServer();
    User user = testUser("petrov");

    XuiInboundsResponse withoutTargetUser = inboundsResponse(List.of(
        inbound(1, 443, "first", "other-user")
    ));
    XuiInboundsResponse withTargetUser = inboundsResponse(List.of(
        inbound(1, 443, "first", "petrov")
    ));

    mockLoginSuccess(3);
    mockAddClientSuccess();
    mockInboundsSequence(withoutTargetUser, withoutTargetUser, withTargetUser);

    String config = service.getVlessConfig(server, user);

    assertThat(config).contains("vless://uuid-petrov@");
    assertThat(config).contains(":443?");
  }

  @Test
  @DisplayName("Должен выбросить XuiUnavailableException, если на сервере нет VLESS inbound")
  void shouldThrowWhenNoVlessInboundFound() {
    Server server = testServer();
    User user = testUser("sidorov");

    XuiInboundsResponse noVless = inboundsResponse(List.of(inboundWithProtocol()));

    mockLoginSuccess(1);
    mockInboundsSequence(noVless);

    assertThatThrownBy(() -> service.createClient(server, user))
        .isInstanceOf(XuiUnavailableException.class)
        .hasMessageContaining("No VLESS inbound found");
  }

  @Test
  @DisplayName("Должен выбросить XuiUnavailableException, если login не вернул cookie")
  void shouldThrowWhenLoginCookieMissing() {
    Server server = testServer();
    User user = testUser("ivanov");

    when(loginResponseSpec.toBodilessEntity()).thenReturn(ResponseEntity.ok().build());

    assertThatThrownBy(() -> service.getVlessConfig(server, user))
        .isInstanceOf(XuiUnavailableException.class)
        .hasMessageContaining("No session cookie");
  }

  private void configureLoginChain() {
    RestClient.RequestBodyUriSpec postUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    loginBodySpec = mock(RestClient.RequestBodySpec.class);
    loginResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.post()).thenReturn(postUriSpec);
    when(postUriSpec.uri("/login")).thenReturn(loginBodySpec);
    when(loginBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(loginBodySpec);
    when(loginBodySpec.body(any(MultiValueMap.class))).thenReturn(loginBodySpec);
    when(loginBodySpec.retrieve()).thenReturn(loginResponseSpec);
  }

  private void configureInboundsChain() {
    RestClient.RequestHeadersUriSpec getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.RequestHeadersSpec getHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    inboundsResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.get()).thenReturn(getUriSpec);
    when(getUriSpec.uri("/panel/api/inbounds/list")).thenReturn(getHeadersSpec);
    when(getHeadersSpec.header(eq(HttpHeaders.COOKIE), any(String[].class))).thenReturn(getHeadersSpec);
    when(getHeadersSpec.retrieve()).thenReturn(inboundsResponseSpec);
  }

  private void configureAddClientChain() {
    RestClient.RequestBodyUriSpec addClientPostUriSpec = mock(RestClient.RequestBodyUriSpec.class);
    RestClient.RequestBodySpec addClientBodySpec = mock(RestClient.RequestBodySpec.class);
    addClientResponseSpec = mock(RestClient.ResponseSpec.class);

    when(restClient.post()).thenReturn(addClientPostUriSpec, addClientPostUriSpec);
    when(addClientPostUriSpec.uri("/panel/api/inbounds/addClient")).thenReturn(addClientBodySpec);
    when(addClientPostUriSpec.uri("/login")).thenReturn(loginBodySpec);
    when(addClientBodySpec.header(eq(HttpHeaders.COOKIE), any(String[].class))).thenReturn(addClientBodySpec);
    when(addClientBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(addClientBodySpec);
    when(addClientBodySpec.body(any(MultiValueMap.class))).thenReturn(addClientBodySpec);
    when(addClientBodySpec.retrieve()).thenReturn(addClientResponseSpec);
  }

  private void mockLoginSuccess(int times) {
    ResponseEntity<Void> response = loginResponseWithCookie();
    var stubbing = when(loginResponseSpec.toBodilessEntity()).thenReturn(response);
    switch (times) {
      case 1 -> {
      }
      case 2 -> stubbing.thenReturn(response);
      case 3 -> stubbing.thenReturn(response).thenReturn(response);
      default -> throw new IllegalArgumentException("Unsupported login calls count: " + times);
    }
  }

  private void mockInboundsSequence(XuiInboundsResponse... responses) {
    if (responses.length == 1) {
      when(inboundsResponseSpec.body(XuiInboundsResponse.class)).thenReturn(responses[0]);
      return;
    }
    when(inboundsResponseSpec.body(XuiInboundsResponse.class))
        .thenReturn(responses[0], java.util.Arrays.copyOfRange(responses, 1, responses.length));
  }

  private void mockAddClientSuccess() {
    when(addClientResponseSpec.body(String.class)).thenReturn("ok");
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
    server.setPassword("pwd");
    server.setWebBasePath("");
    return server;
  }

  private User testUser(String fullName) {
    User user = new User();
    user.setFullName(fullName);
    user.setXuiId(UUID.randomUUID());
    user.setXuiSubscription(UUID.randomUUID());
    user.setUserTelegramId(12345L);
    return user;
  }

  private XuiInboundsResponse inboundsResponse(List<XuiInboundsResponse.Inbound> inbounds) {
    XuiInboundsResponse response = new XuiInboundsResponse();
    response.setSuccess(true);
    response.setObj(inbounds);
    return response;
  }

  private XuiInboundsResponse.Inbound inbound(int id, int port, String remark, String email) {
    XuiInboundsResponse.Inbound inbound = new XuiInboundsResponse.Inbound();
    inbound.setId(id);
    inbound.setPort(port);
    inbound.setProtocol("vless");
    inbound.setTag("tag-" + id);
    inbound.setRemark(remark);
    inbound.setSettings("""
        {"clients":[{"id":"uuid-%s","email":"%s","enable":true,"flow":"xtls-rprx-vision"}]}
        """.formatted(email, email));
    inbound.setStreamSettings("""
        {"network":"grpc","security":"reality","realitySettings":{"settings":{"publicKey":"pk","fingerprint":"chrome","spiderX":"/"},"serverNames":["sni.example"],"shortIds":["ab12"]},"grpcSettings":{"serviceName":"svc"}}
        """);
    return inbound;
  }

  private XuiInboundsResponse.Inbound inboundWithProtocol() {
    XuiInboundsResponse.Inbound inbound = new XuiInboundsResponse.Inbound();
    inbound.setId(10);
    inbound.setPort(443);
    inbound.setProtocol("trojan");
    inbound.setTag("tag-10");
    inbound.setRemark("remark");
    inbound.setSettings("{\"clients\":[]}");
    inbound.setStreamSettings("{\"network\":\"grpc\",\"security\":\"none\"}");
    return inbound;
  }
}

