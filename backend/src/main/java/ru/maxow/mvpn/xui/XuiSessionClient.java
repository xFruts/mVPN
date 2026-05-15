package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.util.HashMap;
import java.util.Map;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class XuiSessionClient {
  RestClient.Builder restClientBuilder;

  XuiSessionClient(RestClient.Builder restClientBuilder) {
    this.restClientBuilder = restClientBuilder;
  }

  RestClient buildPanelClient(Server server) {
    return buildClient(server, buildBaseUrl(server));
  }

  RestClient buildRootClient(Server server) {
    return buildClient(server, buildRootBaseUrl(server));
  }

  String login(RestClient restClient, Server server) {
    if (StringUtils.hasText(server.getXuiAuthToken())) {
      return null;
    }
    return login(restClient, server.getXuiLogin(), server.getXuiPassword());
  }

  private RestClient buildClient(Server server, String baseUrl) {
    RestClient.Builder builder = restClientBuilder.clone().baseUrl(baseUrl);
    if (StringUtils.hasText(server.getXuiAuthToken())) {
      builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + server.getXuiAuthToken());
    }
    return builder.build();
  }

  private String buildBaseUrl(Server server) {
    String baseUrl = String.format("https://%s:%d", server.getIp(), server.getPort());
    if (StringUtils.hasText(server.getWebBasePath())) {
      baseUrl += "/" + server.getWebBasePath();
    }
    return baseUrl;
  }

  private String buildRootBaseUrl(Server server) {
    return String.format("https://%s:%d", server.getIp(), server.getPort());
  }

  private String login(RestClient restClient, String username, String password) {
    Map<String, String> loginRequest = new HashMap<>();
    loginRequest.put("username", username);
    loginRequest.put("password", password);

    ResponseEntity<Void> response = restClient.post()
        .uri("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .body(loginRequest)
        .retrieve()
        .toBodilessEntity();

    String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    if (cookie == null) {
      throw new XuiUnavailableException("Login failed: No session cookie");
    }
    return cookie;
  }
}

