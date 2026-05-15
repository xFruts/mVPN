package ru.maxow.mvpn.xui;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

@Slf4j
@Component
class XuiInboundClient {
  XuiInboundsResponse getInbounds(RestClient restClient, String sessionCookie) {
    RestClient.RequestHeadersSpec<?> request = restClient.get().uri("/panel/api/inbounds/list");
    if (sessionCookie != null) {
      request = request.header(HttpHeaders.COOKIE, sessionCookie);
    }
    return request
        .retrieve()
        .body(XuiInboundsResponse.class);
  }

  void addClientToInbound(
      RestClient restClient,
      String sessionCookie,
      XuiInboundsResponse.Inbound inbound,
      User user,
      Server server,
      String clientSettings) {
    try {
      MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
      formData.add("id", String.valueOf(inbound.getId()));
      formData.add("settings", clientSettings);

      RestClient.RequestBodySpec request = restClient.post().uri("/panel/api/inbounds/addClient");
      if (sessionCookie != null) {
        request = request.header(HttpHeaders.COOKIE, sessionCookie);
      }

      String response = request
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(formData)
          .retrieve()
          .body(String.class);

      log.info(
          "XUI addClient success. serverId={}, serverName={}, inboundId={}, userId={},"
              + " xuiId={}, response={}",
          server.getId(),
          server.getName(),
          inbound.getId(),
          user.getId(),
          user.getXuiId(),
          response);
    } catch (Exception e) {
      log.error(
          "XUI addClient payload serialization failed. serverId={}, userId={}, xuiId={}",
          server.getId(),
          user.getId(),
          user.getXuiId(),
          e);
      throw new XuiUnavailableException("Failed to prepare new client settings");
    }
  }

  void updateClientInInbound(
      RestClient restClient,
      String sessionCookie,
      XuiInboundsResponse.Inbound inbound,
      User user,
      Server server,
      XuiClient existingClient,
      String clientSettings) {
    try {
      MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
      formData.add("id", String.valueOf(inbound.getId()));
      formData.add("settings", clientSettings);

      RestClient.RequestBodySpec request = restClient.post().uri("/panel/api/inbounds/updateClient/" + existingClient.getId());
      if (sessionCookie != null) {
        request = request.header(HttpHeaders.COOKIE, sessionCookie);
      }

      String response = request
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(formData)
          .retrieve()
          .body(String.class);

      log.info(
          "XUI updateClient success. serverId={}, serverName={}, inboundId={}, userId={},"
              + " xuiId={}, response={}",
          server.getId(),
          server.getName(),
          inbound.getId(),
          user.getId(),
          user.getXuiId(),
          response);
    } catch (Exception e) {
      log.error(
          "XUI updateClient payload serialization failed. serverId={}, userId={}, xuiId={}",
          server.getId(),
          user.getId(),
          user.getXuiId(),
          e);
      throw new XuiUnavailableException("Failed to edit client settings");
    }
  }

}

