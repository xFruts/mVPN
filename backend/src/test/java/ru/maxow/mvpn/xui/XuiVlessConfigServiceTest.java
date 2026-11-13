package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("XuiVlessConfigService - Unit тесты")
class XuiVlessConfigServiceTest {

  @Mock private XuiInboundClient inboundClient;
  @Mock private VlessLinkBuilder vlessLinkBuilder;
  @Mock private RestClient restClient;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private XuiVlessConfigService service;

  @BeforeEach
  void setUp() {
    service = new XuiVlessConfigService(inboundClient, vlessLinkBuilder);
  }

  @Test
  @DisplayName("Выбрасывает NotFoundException если клиент не найден в inbound")
  void shouldThrowNotFoundWhenClientNotInSettings() {
    Server server = testServer();
    User user = testUser();
    XuiInboundsResponse.Inbound inbound =
        new XuiInboundsResponse.Inbound(1, 443, "vless", "vless", null, "remark", null);

    when(inboundClient.getInbounds(restClient, "cookie"))
        .thenReturn(new XuiInboundsResponse(true, List.of(inbound)));

    assertThatThrownBy(() -> service.resolveVlessConfig(restClient, "cookie", server, user))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("not found on server");
  }

  @Test
  @DisplayName("Успешно формирует VLESS ссылку когда клиент найден")
  void shouldReturnVlessConfigWhenClientFound() throws Exception {
    Server server = testServer();
    User user = testUser();

    String settingsJson = "{\"clients\":[{\"id\":\"" + user.getXuiId()
        + "\",\"email\":\"" + user.getFullName() + "\"}]}";
    var settingsNode = objectMapper.readTree(settingsJson);

    XuiInboundsResponse.Inbound inbound =
        new XuiInboundsResponse.Inbound(1, 443, "vless", "vless", settingsNode, "remark", null);

    when(inboundClient.getInbounds(restClient, "cookie"))
        .thenReturn(new XuiInboundsResponse(true, List.of(inbound)));
    when(vlessLinkBuilder.generateVlessLink(
        any(), eq(server.getIp()), eq(server.getCountryEmoji()), eq(inbound)))
        .thenReturn("vless://" + user.getXuiId() + "@" + server.getIp() + ":443");

    String link = service.resolveVlessConfig(restClient, "cookie", server, user);

    assertThat(link).isEqualTo("vless://" + user.getXuiId() + "@" + server.getIp() + ":443");
  }

  private Server testServer() {
    Server server = new Server();
    server.setId(10L);
    server.setName("Moscow-1");
    server.setIp("1.2.3.4");
    server.setCountryEmoji("🇷🇺");
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
