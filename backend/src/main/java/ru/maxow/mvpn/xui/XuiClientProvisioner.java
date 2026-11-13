package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.dto.XuiClientDto;

import java.util.List;

/**
 * Сервис управления жизненным циклом (provisioning) клиентов в 3X-UI.
 * <p>
 * Отвечает за операции изменения состояния (Command): регистрацию и обновление на ноде.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XuiClientProvisioner {

  private static final String PROTOCOL_VLESS = "vless";

  XuiClientApiClient clientApiClient;
  XuiInboundClient inboundClient;
  XuiClientDtoFactory clientDtoFactory;

  public void upsertClient(
      RestClient restClient,
      String sessionCookie,
      Server server,
      User user,
      Subscription subscription) {

    String clientEmail = user.getFullName();
    Tariff tariff = subscription.getTariff();

    XuiInboundsResponse inbounds = inboundClient.getInbounds(restClient, sessionCookie);
    List<Integer> vlessInboundIds = findVlessInboundIds(inbounds);

    if (vlessInboundIds.isEmpty()) {
      throw new XuiUnavailableException(
          "No VLESS inbound found on server: " + server.getName());
    }

    XuiClientDto existingClient =
        clientApiClient.getClient(restClient, sessionCookie, clientEmail);
    XuiClientDto clientDto =
        clientDtoFactory.createClientDto(user, subscription, tariff, existingClient);

    if (existingClient != null) {
      clientApiClient.updateClient(restClient, sessionCookie, clientEmail, clientDto, server);
    } else {
      clientApiClient.addClient(restClient, sessionCookie, clientDto, vlessInboundIds, server);
    }
  }

  private List<Integer> findVlessInboundIds(XuiInboundsResponse inbounds) {
    if (inbounds == null || inbounds.obj() == null) {
      return List.of();
    }

    return inbounds.obj().stream()
        .filter(inbound -> PROTOCOL_VLESS.equalsIgnoreCase(inbound.protocol()))
        .map(XuiInboundsResponse.Inbound::id)
        .toList();
  }
}
