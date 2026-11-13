package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.xui.dto.XuiClientDto;

/**
 * Сервис сбора телеметрии и статистики трафика клиентов в 3X-UI.
 * <p>
 * Отвечает за чтение метрик потребления трафика (Query) и нормализацию ответа панели.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XuiClientTrafficReader {

  XuiClientApiClient clientApiClient;

  public XuiClientTraffic getClientTraffic(
      RestClient restClient,
      String sessionCookie,
      Server server,
      String clientEmail) {

    XuiClientDto client = clientApiClient.getClient(restClient, sessionCookie, clientEmail);
    if (client == null) {
      log.warn("Client not found on XUI for traffic query. serverId={}, email={}",
          server.getId(), clientEmail);
    }
    return XuiClientTraffic.from(clientEmail, client);
  }
}
