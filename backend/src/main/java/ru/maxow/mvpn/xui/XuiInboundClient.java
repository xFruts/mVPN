package ru.maxow.mvpn.xui;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

/**
 * HTTP-клиент для чтения конфигурации inbound'ов {@code /panel/api/inbounds/*}.
 * <p>
 * После миграции на Clients API мутации клиентов выполняются через {@link XuiClientApiClient}.
 * Данный клиент используется только для получения списка inbound'ов и их настроек
 * (streamSettings, settings) — необходимо для генерации VLESS-ссылок и определения доступных inbound'ов.
 */
@Slf4j
@Component
class XuiInboundClient {

  static {
    new ParameterizedTypeReference<>() {};
  }

  /**
   * Получает список всех inbound'ов панели.
   */
  XuiInboundsResponse getInbounds(RestClient restClient, String sessionCookie) {
    RestClient.RequestHeadersSpec<?> request = restClient.get().uri("/panel/api/inbounds/list");
    if (sessionCookie != null) {
      request = request.header(HttpHeaders.COOKIE, sessionCookie);
    }

    XuiInboundsResponse response = request.retrieve().body(XuiInboundsResponse.class);
    if (response == null) {
      log.warn("No response from /panel/api/inbounds/list");
      throw new XuiUnavailableException("No response from API");
    }
    return response;
  }
}
