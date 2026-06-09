package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.user.User;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("XuiPanelServiceImpl - Unit тесты")
class XuiPanelServiceImplTest {

  @Mock private XuiSessionClient sessionClient;
  @Mock private XuiClientApiClient clientApiClient;
  @Mock private XuiInboundClient inboundClient;
  @Mock private XuiJsonConfigClient jsonConfigClient;
  @Mock private VlessLinkBuilder vlessLinkBuilder;
  @Mock private RestClient restClient;

  private XuiPanelServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new XuiPanelServiceImpl(
        sessionClient,
        clientApiClient,
        inboundClient,
        jsonConfigClient,
        vlessLinkBuilder);

    when(sessionClient.buildPanelClient(any())).thenReturn(restClient);
    when(sessionClient.buildRootClient(any())).thenReturn(restClient);
  }

  @Test
  @DisplayName("Возвращает конфиг, когда клиент найден")
  void shouldReturnConfigWhenClientFound() {
    Server server = testServer();
    User user = testUser();
    XuiInboundsResponse.Inbound inbound = new XuiInboundsResponse.Inbound(1, 443, "vless", "vless", null, "remark", null);

    when(sessionClient.login(eq(restClient), eq(server))).thenReturn("session=ok");
    when(inboundClient.getInbounds(restClient, "session=ok"))
        .thenReturn(new XuiInboundsResponse(true, List.of(inbound)));

    assertThatThrownBy(() -> service.getVlessConfig(server, user))
        .isInstanceOf(ru.maxow.mvpn.util.exception.NotFoundException.class);
    verify(sessionClient).login(restClient, server);
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
}
