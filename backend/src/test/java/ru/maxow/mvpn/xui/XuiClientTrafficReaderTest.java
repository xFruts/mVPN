package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.xui.dto.XuiClientDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("XuiClientTrafficReader - Unit тесты")
class XuiClientTrafficReaderTest {

  @Mock private XuiClientApiClient clientApiClient;
  @Mock private RestClient restClient;

  @InjectMocks
  private XuiClientTrafficReader trafficReader;

  @Test
  @DisplayName("getClientTraffic: успешно возвращает статистику трафика клиента")
  void shouldReturnClientTrafficWhenFound() {
    Server server = new Server();
    server.setId(10L);
    server.setName("Frankfurt-1");

    String email = "user@mvpn.io";
    String sessionCookie = "session=test";

    XuiClientDto clientDto = new XuiClientDto(
        "id-1", email, "sub-1", 1000L, 5000L, true, null, 2, "flow",
        null, null, null, null, new XuiClientDto.TrafficInfo(1500L, 3500L, true)
    );

    when(clientApiClient.getClient(restClient, sessionCookie, email)).thenReturn(clientDto);

    XuiClientTraffic traffic =
        trafficReader.getClientTraffic(restClient, sessionCookie, server, email);

    assertThat(traffic).isNotNull();
    assertThat(traffic.email()).isEqualTo(email);
    assertThat(traffic.upload()).isEqualTo(1500L);
    assertThat(traffic.download()).isEqualTo(3500L);
    assertThat(traffic.total()).isEqualTo(5000L);
    verify(clientApiClient).getClient(restClient, sessionCookie, email);
  }

  @Test
  @DisplayName("getClientTraffic: возвращает пустой трафик, если клиент не найден на ноде")
  void shouldReturnEmptyTrafficWhenClientNotFound() {
    Server server = new Server();
    server.setId(10L);
    server.setName("Frankfurt-1");

    String email = "missing@mvpn.io";
    String sessionCookie = "session=test";

    when(clientApiClient.getClient(restClient, sessionCookie, email)).thenReturn(null);

    XuiClientTraffic traffic =
        trafficReader.getClientTraffic(restClient, sessionCookie, server, email);

    assertThat(traffic).isNotNull();
    assertThat(traffic.email()).isEqualTo(email);
    assertThat(traffic.upload()).isZero();
    assertThat(traffic.download()).isZero();
    assertThat(traffic.total()).isZero();
  }

  @Test
  @DisplayName("parseTrafficNode: парсит 3X-UI с root полями up/down")
  void shouldParseStandard3xUiObjectPayload() throws Exception {
    tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
    String json = """
        {
          "id": 1,
          "inboundId": 1,
          "enable": true,
          "email": "user@mvpn.io",
          "up": 1048576,
          "down": 5242880,
          "expiryTime": 1735689600000,
          "total": 107374182400
        }
        """;
    tools.jackson.databind.JsonNode node = mapper.readTree(json);

    XuiClientTraffic traffic = trafficReader.parseTrafficNode(node, "user@mvpn.io");

    assertThat(traffic).isNotNull();
    assertThat(traffic.email()).isEqualTo("user@mvpn.io");
    assertThat(traffic.upload()).isEqualTo(1048576L);
    assertThat(traffic.download()).isEqualTo(5242880L);
    assertThat(traffic.total()).isEqualTo(1048576L + 5242880L);
    assertThat(traffic.expiryTime()).isEqualTo(1735689600000L);
  }

  @Test
  @DisplayName("parseTrafficNode: суммирует трафик при массиве инбаундов")
  void shouldAggregateTrafficFromArrayPayload() throws Exception {
    tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
    String json = """
        [
          {"email": "user@mvpn.io", "up": 1000, "down": 2000, "total": 10000, "expiryTime": 500},
          {"email": "user@mvpn.io", "up": 3000, "down": 4000, "total": 20000, "expiryTime": 600}
        ]
        """;
    tools.jackson.databind.JsonNode node = mapper.readTree(json);

    XuiClientTraffic traffic = trafficReader.parseTrafficNode(node, "user@mvpn.io");

    assertThat(traffic).isNotNull();
    assertThat(traffic.upload()).isEqualTo(4000L);
    assertThat(traffic.download()).isEqualTo(6000L);
    assertThat(traffic.total()).isEqualTo(10000L);
    assertThat(traffic.expiryTime()).isEqualTo(600L);
  }

  @Test
  @DisplayName("XuiClientTraffic.from: поддерживает root поля up/down из DTO")
  void shouldExtractRootTrafficFromDto() {
    XuiClientDto clientDto = new XuiClientDto(
        "uuid", "user@mvpn.io", "sub-1", 100L, 500L, true,
        null, 2, null, null, null, null, null, null, 1200L, 3400L
    );

    XuiClientTraffic traffic = XuiClientTraffic.from("user@mvpn.io", clientDto);

    assertThat(traffic.upload()).isEqualTo(1200L);
    assertThat(traffic.download()).isEqualTo(3400L);
    assertThat(traffic.total()).isEqualTo(4600L);
  }
}
