package ru.maxow.mvpn.xui;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.dto.XuiApiResponse;
import ru.maxow.mvpn.xui.dto.XuiClientDto;
import ru.maxow.mvpn.xui.dto.XuiCreateUpdateClientRequestDto;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * HTTP-клиент для работы с эндпоинтами {@code /panel/api/clients/*} (3X-UI v3).
 * <p>
 * Клиенты в новом API — самостоятельные сущности, привязанные к inbound'ам через {@code inboundIds}.
 * Секреты (UUID, password) генерируются сервером автоматически, если не переданы.
 */
@Slf4j
@Component
class XuiClientApiClient {

  private static final ParameterizedTypeReference<XuiApiResponse<XuiClientDto>> CLIENT_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};

  /**
   * Получает клиента по email.
   *
   * @return клиент или {@code null}, если не найден (404 / success=false)
   */
  XuiClientDto getClient(RestClient restClient, String sessionCookie, String email) {
    String encodedEmail = UriUtils.encodePathSegment(email, StandardCharsets.UTF_8);

    try {
      XuiApiResponse<XuiClientDto> response = doGet(
          restClient, sessionCookie,
          "/panel/api/clients/get/" + encodedEmail,
          CLIENT_RESPONSE_TYPE
      );
      if (response == null || !response.success()) {
        return null;
      }
      return response.obj();
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404) {
        return null;
      }
      throw new XuiUnavailableException("Failed to get client: " + email, e);
    }
  }

  /**
   * Создаёт нового клиента и привязывает к указанным inbound'ам.
   *
   * @throws XuiUnavailableException при ошибке API
   */
  void addClient(RestClient restClient, String sessionCookie,
                 XuiClientDto clientDto, List<Integer> inboundIds, Server server) {
    XuiCreateUpdateClientRequestDto requestBody = new XuiCreateUpdateClientRequestDto(clientDto, inboundIds);

    log.info("XUI addClient: email={}, inboundIds={}, serverId={}",
        clientDto.email(), inboundIds, server.getId());

    XuiApiResponse<?> response = doPost(
        restClient, sessionCookie,
        "/panel/api/clients/add",
        requestBody
    );

    if (response == null || !response.success()) {
      String msg = response != null ? response.msg() : "No response";
      log.error("XUI addClient failed: serverId={}, email={}, msg={}",
          server.getId(), clientDto.email(), msg);
      throw new XuiUnavailableException("Failed to add client: " + msg);
    }

    log.info("XUI addClient success: serverId={}, email={}", server.getId(), clientDto.email());
  }

  /**
   * Обновляет существующего клиента по email. Изменения применяются ко всем привязанным inbound'ам.
   *
   * @throws XuiUnavailableException при ошибке API
   */
  void updateClient(RestClient restClient, String sessionCookie,
                    String currentEmail, XuiClientDto clientDto, Server server) {
    String encodedEmail = UriUtils.encodePathSegment(currentEmail, StandardCharsets.UTF_8);

    log.info("XUI updateClient: email={}, serverId={}", currentEmail, server.getId());

    XuiApiResponse<?> response = doPost(
        restClient, sessionCookie,
        "/panel/api/clients/update/" + encodedEmail,
        clientDto
    );

    if (response == null || !response.success()) {
      String msg = response != null ? response.msg() : "No response";
      log.error("XUI updateClient failed: serverId={}, email={}, msg={}",
          server.getId(), currentEmail, msg);
      throw new XuiUnavailableException("Failed to update client: " + msg);
    }

    log.info("XUI updateClient success: serverId={}, email={}", server.getId(), currentEmail);
  }

  private <T> T doGet(RestClient restClient, String sessionCookie,
                      String uri, ParameterizedTypeReference<T> type) {
    RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uri);
    if (sessionCookie != null) {
      request = request.header(HttpHeaders.COOKIE, sessionCookie);
    }
    return request.retrieve().body(type);
  }

  private XuiApiResponse<?> doPost(RestClient restClient, String sessionCookie,
                                   String uri, Object body) {
    RestClient.RequestBodySpec request = restClient.post().uri(uri);
    if (sessionCookie != null) {
      request = request.header(HttpHeaders.COOKIE, sessionCookie);
    }
    return request
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {
        });
  }
}
