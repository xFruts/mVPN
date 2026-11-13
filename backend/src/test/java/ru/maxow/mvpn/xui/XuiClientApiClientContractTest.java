package ru.maxow.mvpn.xui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.xui.dto.XuiClientDto;

@DisplayName("XuiClientApiClient - HTTP Contract & Serialization Tests")
class XuiClientApiClientContractTest {

  private RestClient restClient;
  private MockRestServiceServer mockServer;
  private XuiClientApiClient clientApiClient;
  private Server server;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://10.0.0.1:2053");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    restClient = builder.build();
    clientApiClient = new XuiClientApiClient();

    server = new Server();
    server.setId(1L);
    server.setName("Test Node");
  }

  @Test
  @DisplayName("addClient: сериализует тело запроса с обёрткой client и inboundIds")
  void shouldSerializeWrappedRequestBodyForAddClient() {
    XuiClientDto clientDto = new XuiClientDto(
        "uuid-123",
        "test_testovich",
        "sub-456",
        107374182400L,
        1740000000000L,
        true,
        123456789L,
        2,
        "xtls-rprx-vision",
        null,
        null,
        null,
        null,
        null
    );

    mockServer.expect(requestTo("https://10.0.0.1:2053/panel/api/clients/add"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Cookie", "session=valid-cookie"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {
              "client": {
                "id": "uuid-123",
                "email": "test_testovich",
                "subId": "sub-456",
                "totalGB": 107374182400,
                "expiryTime": 1740000000000,
                "enable": true,
                "tgId": 123456789,
                "limitIp": 2,
                "flow": "xtls-rprx-vision"
              },
              "inboundIds": [1, 2]
            }
            """))
        .andRespond(withSuccess("{\"success\":true,\"msg\":\"Client added\"}",
            MediaType.APPLICATION_JSON));

    clientApiClient.addClient(restClient, "session=valid-cookie", clientDto, List.of(1, 2), server);
    mockServer.verify();
  }

  @Test
  @DisplayName("updateClient: отправляет плоский JSON клиента напрямую в корне запроса без обёртки")
  void shouldSerializeFlatClientDtoForUpdateClient() {
    XuiClientDto clientDto = new XuiClientDto(
        "uuid-123",
        "test_testovich",
        "sub-456",
        214748364800L,
        1750000000000L,
        true,
        123456789L,
        3,
        "xtls-rprx-vision",
        null,
        null,
        null,
        null,
        null
    );

    mockServer.expect(requestTo("https://10.0.0.1:2053/panel/api/clients/update/test_testovich"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Cookie", "session=valid-cookie"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("""
            {
              "id": "uuid-123",
              "email": "test_testovich",
              "subId": "sub-456",
              "totalGB": 214748364800,
              "expiryTime": 1750000000000,
              "enable": true,
              "tgId": 123456789,
              "limitIp": 3,
              "flow": "xtls-rprx-vision"
            }
            """))
        .andRespond(withSuccess("{\"success\":true,\"msg\":\"Client updated\"}",
            MediaType.APPLICATION_JSON));

    clientApiClient.updateClient(restClient, "session=valid-cookie", "test_testovich",
        clientDto, server);
    mockServer.verify();
  }

  @Test
  @DisplayName("getClient: корректно декодирует ответ XuiApiResponse<XuiClientDto>")
  void shouldDeserializeClientResponse() {
    mockServer.expect(requestTo("https://10.0.0.1:2053/panel/api/clients/get/test_testovich"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Cookie", "session=valid-cookie"))
        .andRespond(withSuccess("""
            {
              "success": true,
              "msg": "Success",
              "obj": {
                "id": "uuid-123",
                "email": "test_testovich",
                "totalGB": 107374182400,
                "expiryTime": 1740000000000,
                "enable": true,
                "traffic": {
                  "up": 1024,
                  "down": 2048,
                  "enable": true
                }
              }
            }
            """, MediaType.APPLICATION_JSON));

    XuiClientDto result = clientApiClient.getClient(restClient, "session=valid-cookie",
        "test_testovich");

    assertThat(result).isNotNull();
    assertThat(result.uuid()).isEqualTo("uuid-123");
    assertThat(result.email()).isEqualTo("test_testovich");
    assertThat(result.totalGB()).isEqualTo(107374182400L);
    assertThat(result.traffic()).isNotNull();
    assertThat(result.traffic().up()).isEqualTo(1024L);
    assertThat(result.traffic().down()).isEqualTo(2048L);
    mockServer.verify();
  }
}
