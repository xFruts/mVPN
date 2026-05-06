package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("XuiPanelServiceImpl - Unit тесты")
class XuiPanelServiceImplTest {

  @Mock
  private XuiSessionClient sessionClient;
  @Mock
  private XuiInboundClient inboundClient;
  @Mock
  private XuiJsonConfigClient jsonConfigClient;
  @Mock
  private RestClient restClient;
  @Mock
  private SubscriptionService subscriptionService;
  @Mock
  private ObjectProvider<RequestScopedSubPortCache> subPortCacheProvider;
  @Mock
  private XuiClientPayloadBuilder payloadBuilder;
  @Mock
  private XuiInboundMutator inboundMutator;
  @Mock
  private VlessLinkBuilder vlessLinkBuilder;
  @Mock
  private XuiTrafficMapper trafficMapper;

  private XuiPanelServiceImpl service;

  private RestClient.ResponseSpec trafficResponseSpec;

  @BeforeEach
  void setUp() {
    service = new XuiPanelServiceImpl(
        sessionClient,
        inboundClient,
        jsonConfigClient,
        subscriptionService,
        subPortCacheProvider,
        payloadBuilder,
        inboundMutator,
        vlessLinkBuilder,
        trafficMapper);

    when(subPortCacheProvider.getIfAvailable()).thenReturn(null);
    when(sessionClient.buildPanelClient(any())).thenReturn(restClient);
    when(sessionClient.buildRootClient(any())).thenReturn(restClient);

    configureTrafficChain();
  }

  @Test
  @DisplayName("Возвращает конфиг, когда клиент найден")
  void shouldReturnConfigWhenClientFound() throws JsonProcessingException {
    Server server = testServer();
    User user = testUser("test-user");
    Subscription sub = activeSubscription();
    XuiInboundsResponse.Inbound inbound = inbound(1, 443, "vless");
    XuiClient client = new XuiClient();
    client.setId(user.getXuiId().toString());

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    when(inboundClient.getInbounds(restClient, "session=ok"))
        .thenReturn(inboundsResponse(List.of(inbound)));
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId())).thenReturn(sub);
    when(inboundMutator.findClientInInbound(any(), eq(user))).thenReturn(Optional.of(client));

    ObjectNode payload = new ObjectMapper().createObjectNode();
    when(payloadBuilder.buildClientPayload(eq(user), eq(sub), any(), eq(client))).thenReturn(payload);
    when(payloadBuilder.wrapClientPayload(payload)).thenReturn("{\"clients\":[]}");

    when(vlessLinkBuilder.generateVlessLink(eq(client), eq(server.getIp()), any(), eq(inbound)))
        .thenReturn("vless://link");

    String config = service.getVlessConfig(server, user);

    assertThat(config).isEqualTo("vless://link");
    verify(inboundClient).updateClientInInbound(restClient, "session=ok", inbound, user, server,
        client, "{\"clients\":[]}");
    verify(inboundMutator).applyClientPayloadToInbound(eq(inbound), eq(payload), eq(client));
  }

  @Test
  @DisplayName("Делает addClient, когда клиента нет")
  void shouldAddClientWhenMissing() throws JsonProcessingException {
    Server server = testServer();
    User user = testUser("new-user");
    Subscription sub = activeSubscription();
    XuiInboundsResponse.Inbound inbound = inbound(1, 443, "vless");

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    when(inboundClient.getInbounds(restClient, "session=ok"))
        .thenReturn(inboundsResponse(List.of(inbound)));
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId())).thenReturn(sub);
    XuiClient client = new XuiClient();
    client.setId(user.getXuiId().toString());
    when(inboundMutator.findClientInInbound(any(), eq(user))).thenReturn(Optional.empty(), Optional.of(client));

    ObjectNode payload = new ObjectMapper().createObjectNode();
    when(payloadBuilder.buildClientPayload(eq(user), eq(sub), any(), eq(null))).thenReturn(payload);
    when(payloadBuilder.wrapClientPayload(payload)).thenReturn("{\"clients\":[]}");

    when(vlessLinkBuilder.generateVlessLink(any(), eq(server.getIp()), any(), eq(inbound)))
        .thenReturn("vless://new-link");

    String config = service.getVlessConfig(server, user);

    assertThat(config).isEqualTo("vless://new-link");
    verify(inboundClient).addClientToInbound(restClient, "session=ok", inbound, user, server,
        "{\"clients\":[]}");
    verify(inboundMutator).applyClientPayloadToInbound(eq(inbound), eq(payload), eq(null));
  }

  @Test
  @DisplayName("Бросает XuiUnavailableException, если на сервере нет VLESS inbound")
  void shouldThrowWhenNoVlessInboundFound() {
    Server server = testServer();
    User user = testUser("ivanov");

    XuiInboundsResponse noVless = inboundsResponse(List.of(inbound(1, 443, "trojan")));

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    when(inboundClient.getInbounds(restClient, "session=ok")).thenReturn(noVless);

    assertThatThrownBy(() -> service.createClient(server, user))
        .isInstanceOf(XuiUnavailableException.class)
        .hasMessageContaining("No VLESS inbound found");
  }

  @Test
  @DisplayName("Бросает XuiUnavailableException, если login не вернул cookie")
  void shouldThrowWhenLoginCookieMissing() {
    Server server = testServer();
    User user = testUser("ivanov");

    when(sessionClient.login(eq(restClient), eq(server)))
        .thenThrow(new XuiUnavailableException("Login failed: No session cookie"));

    assertThatThrownBy(() -> service.getVlessConfig(server, user))
        .isInstanceOf(XuiUnavailableException.class)
        .hasMessageContaining("No session cookie");
  }

  @Test
  @DisplayName("Возвращает JSON конфиг после подготовки клиента")
  void shouldReturnJsonConfig() throws JsonProcessingException {
    Server server = testServer();
    User user = testUser("json-user");
    Subscription sub = activeSubscription();
    XuiInboundsResponse.Inbound inbound = inbound(1, 443, "vless");
    XuiClient client = new XuiClient();

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    when(inboundClient.getInbounds(restClient, "session=ok"))
        .thenReturn(inboundsResponse(List.of(inbound)));
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId())).thenReturn(sub);
    when(inboundMutator.findClientInInbound(any(), eq(user))).thenReturn(Optional.of(client));

    ObjectNode payload = new ObjectMapper().createObjectNode();
    when(payloadBuilder.buildClientPayload(any(), any(), any(), any())).thenReturn(payload);
    when(payloadBuilder.wrapClientPayload(payload)).thenReturn("{}");

    when(jsonConfigClient.resolveSubscriptionPort(restClient, "session=ok", server)).thenReturn(2096);
    when(jsonConfigClient.buildJsonSubscriptionUrl(eq(server), eq(user), eq(2096)))
        .thenReturn("https://1.2.3.4:2096/json/id");
    when(jsonConfigClient.fetchJsonConfigAtPath(restClient, "session=ok",
        "https://1.2.3.4:2096/json/id", server)).thenReturn("{\"remarks\":\"old\"}");
    when(jsonConfigClient.replaceRemarksWithServerName("{\"remarks\":\"old\"}", server))
        .thenReturn("{\"remarks\":\"Moscow-1\"}");

    String config = service.getJsonConfig(server, user);

    assertThat(config).contains("Moscow-1");
  }

  @Test
  @DisplayName("Повторный вызов getJsonConfig использует request-scoped subPort кэш")
  void shouldReuseSubPortFromRequestCacheOnRepeatedCall() throws JsonProcessingException {
    Server server = testServer();
    User user = testUser("cache-user");
    Subscription sub = activeSubscription();
    XuiInboundsResponse.Inbound inbound = inbound(1, 443, "vless");
    XuiClient client = new XuiClient();

    // подготовим мок кэша и провайдера, чтобы сначала возвращался null, затем - значение из кэша
    RequestScopedSubPortCache cache = mock(RequestScopedSubPortCache.class);
    when(subPortCacheProvider.getIfAvailable()).thenReturn(cache);
    when(cache.get(server.getId())).thenReturn(null, 2096);

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    when(inboundClient.getInbounds(restClient, "session=ok"))
        .thenReturn(inboundsResponse(List.of(inbound)));
    when(subscriptionService.findLastSubscriptionEntityByUserId(user.getId())).thenReturn(sub);
    when(inboundMutator.findClientInInbound(any(), eq(user))).thenReturn(Optional.of(client));

    ObjectNode payload = new ObjectMapper().createObjectNode();
    when(payloadBuilder.buildClientPayload(any(), any(), any(), any())).thenReturn(payload);
    when(payloadBuilder.wrapClientPayload(payload)).thenReturn("{}");

    when(jsonConfigClient.resolveSubscriptionPort(restClient, "session=ok", server)).thenReturn(2096);
    when(jsonConfigClient.buildJsonSubscriptionUrl(eq(server), eq(user), eq(2096)))
        .thenReturn("https://1.2.3.4:2096/json/id");
    when(jsonConfigClient.fetchJsonConfigAtPath(restClient, "session=ok",
        "https://1.2.3.4:2096/json/id", server)).thenReturn("{\"remarks\":\"old\"}");
    when(jsonConfigClient.replaceRemarksWithServerName("{\"remarks\":\"old\"}", server))
        .thenReturn("{\"remarks\":\"Moscow-1\"}");

    // Первый вызов — должен вызвать resolveSubscriptionPort и положить значение в кэш
    String first = service.getJsonConfig(server, user);
    assertThat(first).contains("Moscow-1");

    // Второй вызов — кэш уже содержит значение, resolveSubscriptionPort вызываться не должен
    String second = service.getJsonConfig(server, user);
    assertThat(second).contains("Moscow-1");

    verify(jsonConfigClient, times(1)).resolveSubscriptionPort(restClient, "session=ok", server);
    verify(cache, times(1)).put(server.getId(), 2096);
  }

  @Test
  @DisplayName("Вызывает trafficMapper при получении трафика")
  void shouldCallTrafficMapper() {
    Server server = testServer();
    String clientId = "client-123";
    XuiClientTraffic expected = new XuiClientTraffic();

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    when(trafficResponseSpec.body(String.class)).thenReturn("raw-json");
    when(trafficMapper.mapTrafficResponse("raw-json", server, clientId)).thenReturn(expected);

    XuiClientTraffic result = service.getClientTraffic(server, clientId);

    assertThat(result).isSameAs(expected);
  }

  private void configureTrafficChain() {
    RestClient.RequestHeadersUriSpec<?> getUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
    RestClient.RequestHeadersSpec<?> trafficHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
    trafficResponseSpec = mock(RestClient.ResponseSpec.class);

    doReturn(getUriSpec).when(restClient).get();
    // use doReturn to avoid generics mismatch in Mockito when(...).thenReturn(...) with wildcards
    doReturn(trafficHeadersSpec).when(getUriSpec).uri(anyString());
    doReturn(trafficHeadersSpec).when(trafficHeadersSpec).header(eq(HttpHeaders.COOKIE), anyString());
    doReturn(trafficResponseSpec).when(trafficHeadersSpec).retrieve();
  }


  private Server testServer() {
    Server server = new Server();
    server.setId(10L);
    server.setName("Moscow-1");
    server.setIp("1.2.3.4");
    server.setPort(443);
    server.setXuiLogin("xui");
    server.setXuiPassword("xui-pass");
    server.setCountryEmoji("");
    return server;
  }

  private User testUser(String fullName) {
    User user = new User();
    user.setId(101L);
    user.setFullName(fullName);
    user.setXuiId(UUID.randomUUID());
    user.setXuiSubscription(UUID.randomUUID());
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

  private XuiInboundsResponse.Inbound inbound(int id, int port, String protocol) {
    XuiInboundsResponse.Inbound inbound = new XuiInboundsResponse.Inbound();
    inbound.setId(id);
    inbound.setPort(port);
    inbound.setProtocol(protocol);
    return inbound;
  }
}
