package ru.maxow.mvpn.xui;

import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
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

/**
 * HTTP-клиент для работы с эндпоинтами {@code /panel/api/clients/*} (3X-UI v3).
 * <p>
 * Клиенты в новом API — самостоятельные сущности, привязанные к inbound'ам.
 * Секреты (UUID, password) генерируются сервером автоматически, если не переданы.
 */
@Slf4j
@Component
class XuiClientApiClient {

  private static final ParameterizedTypeReference<XuiApiResponse<XuiClientDto>>
      CLIENT_RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

  XuiClientDto getClient(RestClient restClient, String sessionCookie, String email) {
    String encodedEmail = UriUtils.encodePathSegment(email, StandardCharsets.UTF_8);

    try {
      XuiApiResponse<XuiClientDto> response = sendGetRequest(
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

  void addClient(RestClient restClient, String sessionCookie,
                 XuiClientDto clientDto, List<Integer> inboundIds, Server server) {
    XuiCreateUpdateClientRequestDto requestBody =
        new XuiCreateUpdateClientRequestDto(clientDto, inboundIds);

    log.debug("XUI addClient: email={}, inboundIds={}, serverId={}",
        clientDto.email(), inboundIds, server.getId());

    XuiApiResponse<?> response = sendPostRequest(
        restClient, sessionCookie,
        "/panel/api/clients/add",
        requestBody
    );

    if (response == null || !response.success()) {
      String msg = response != null ? response.msg() : "empty response";
      log.warn("XUI addClient failed: serverId={}, email={}, msg={}",
          server.getId(), clientDto.email(), msg);
      throw new XuiUnavailableException(
          String.format("Failed to add client '%s' on XUI server '%s': %s",
              clientDto.email(), server.getName(), msg));
    }
  }

  void updateClient(RestClient restClient, String sessionCookie,
                    String currentEmail, XuiClientDto clientDto, Server server) {
    String encodedEmail = UriUtils.encodePathSegment(currentEmail, StandardCharsets.UTF_8);

    XuiCreateUpdateClientRequestDto requestBody =
        new XuiCreateUpdateClientRequestDto(clientDto, null);

    log.debug("XUI updateClient: currentEmail={}, serverId={}",
        currentEmail, server.getId());

    XuiApiResponse<?> response = sendPostRequest(
        restClient, sessionCookie,
        "/panel/api/clients/update/" + encodedEmail,
        requestBody
    );

    if (response == null || !response.success()) {
      String msg = response != null ? response.msg() : "empty response";
      log.warn("XUI updateClient failed: serverId={}, email={}, msg={}",
          server.getId(), currentEmail, msg);
      throw new XuiUnavailableException(
          String.format("Failed to update client '%s' on XUI server '%s': %s",
              currentEmail, server.getName(), msg));
    }
  }

  private <T> XuiApiResponse<T> sendGetRequest(
      RestClient restClient,
      String sessionCookie,
      String uri,
      ParameterizedTypeReference<XuiApiResponse<T>> typeRef) {

    return restClient.get()
        .uri(uri)
        .header("Cookie", sessionCookie)
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(typeRef);
  }

  private XuiApiResponse<?> sendPostRequest(
      RestClient restClient,
      String sessionCookie,
      String uri,
      Object body) {

    return restClient.post()
        .uri(uri)
        .header("Cookie", sessionCookie)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(XuiApiResponse.class);
  }
}
