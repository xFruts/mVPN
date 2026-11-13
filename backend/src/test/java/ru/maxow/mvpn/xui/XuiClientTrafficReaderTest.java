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
}
