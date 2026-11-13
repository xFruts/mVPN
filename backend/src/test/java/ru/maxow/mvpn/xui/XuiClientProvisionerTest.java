package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.dto.XuiClientDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("XuiClientProvisioner - Unit тесты")
class XuiClientProvisionerTest {

  @Mock private XuiClientApiClient clientApiClient;
  @Mock private XuiInboundClient inboundClient;
  @Mock private XuiClientDtoFactory clientDtoFactory;
  @Mock private RestClient restClient;

  @InjectMocks
  private XuiClientProvisioner provisioner;

  @Test
  @DisplayName("upsertClient: создаёт нового клиента, если он отсутствует на сервере")
  void shouldAddNewClientWhenNotExists() {
    Server server = new Server();
    server.setId(1L);
    server.setName("Node-1");

    User user = new User();
    user.setFullName("new@mvpn.io");
    user.setXuiId(UUID.randomUUID());

    Tariff tariff = new Tariff();
    tariff.setTrafficLimitGb(100);

    Subscription subscription = new Subscription();
    subscription.setTariff(tariff);
    subscription.setEndDate(OffsetDateTime.now());

    XuiInboundsResponse inbounds = new XuiInboundsResponse(
        true,
        List.of(new XuiInboundsResponse.Inbound(10, 443, "tag", "vless", null, "rem", null))
    );
    XuiClientDto newDto = createDto("new@mvpn.io", 0L);

    when(inboundClient.getInbounds(restClient, "session")).thenReturn(inbounds);
    when(clientApiClient.getClient(restClient, "session", "new@mvpn.io")).thenReturn(null);
    when(clientDtoFactory.createClientDto(user, subscription, tariff, null)).thenReturn(newDto);

    provisioner.upsertClient(restClient, "session", server, user, subscription);

    verify(clientApiClient).addClient(restClient, "session", newDto, List.of(10), server);
    verify(clientApiClient, never()).updateClient(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("upsertClient: обновляет клиента, если он уже существует на сервере")
  void shouldUpdateClientWhenAlreadyExists() {
    Server server = new Server();
    server.setId(1L);
    server.setName("Node-1");

    User user = new User();
    user.setFullName("existing@mvpn.io");
    user.setXuiId(UUID.randomUUID());

    Tariff tariff = new Tariff();
    tariff.setTrafficLimitGb(100);

    Subscription subscription = new Subscription();
    subscription.setTariff(tariff);
    subscription.setEndDate(OffsetDateTime.now());

    XuiInboundsResponse inbounds = new XuiInboundsResponse(
        true,
        List.of(new XuiInboundsResponse.Inbound(10, 443, "tag", "vless", null, "rem", null))
    );
    XuiClientDto existingDto = createDto("existing@mvpn.io", 0L);
    XuiClientDto updatedDto = createDto("existing@mvpn.io", 100L);

    when(inboundClient.getInbounds(restClient, "session")).thenReturn(inbounds);
    when(clientApiClient.getClient(restClient, "session", "existing@mvpn.io"))
        .thenReturn(existingDto);
    when(clientDtoFactory.createClientDto(user, subscription, tariff, existingDto))
        .thenReturn(updatedDto);

    provisioner.upsertClient(restClient, "session", server, user, subscription);

    verify(clientApiClient)
        .updateClient(restClient, "session", "existing@mvpn.io", updatedDto, server);
    verify(clientApiClient, never()).addClient(any(), any(), any(), any(), any());
  }

  @Test
  @DisplayName("upsertClient: ошибка если на сервере нет VLESS inbound")
  void shouldThrowExceptionWhenNoVlessInbound() {
    Server server = new Server();
    server.setId(1L);
    server.setName("Node-1");

    User user = new User();
    user.setFullName("user@mvpn.io");

    Tariff tariff = new Tariff();
    Subscription subscription = new Subscription();
    subscription.setTariff(tariff);

    XuiInboundsResponse inbounds = new XuiInboundsResponse(
        true,
        List.of(new XuiInboundsResponse.Inbound(10, 443, "tag", "trojan", null, "rem", null))
    );

    when(inboundClient.getInbounds(restClient, "session")).thenReturn(inbounds);

    assertThatThrownBy(() ->
        provisioner.upsertClient(restClient, "session", server, user, subscription))
        .isInstanceOf(XuiUnavailableException.class)
        .hasMessageContaining("No VLESS inbound found on server: Node-1");
  }

  private XuiClientDto createDto(String email, Long limit) {
    return new XuiClientDto(
        "id", email, "sub", limit, limit, true, null, 1, "flow",
        null, null, null, null, null
    );
  }
}
