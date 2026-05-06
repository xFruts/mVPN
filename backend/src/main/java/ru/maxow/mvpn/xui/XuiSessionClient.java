package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class XuiSessionClient {
  RestClient.Builder restClientBuilder;

  XuiSessionClient(RestClient.Builder restClientBuilder) {
    this.restClientBuilder = restClientBuilder;
  }

  RestClient buildPanelClient(Server server) {
    return restClientBuilder.baseUrl(buildBaseUrl(server)).build();
  }

  RestClient buildRootClient(Server server) {
    return restClientBuilder.baseUrl(buildRootBaseUrl(server)).build();
  }

  String login(RestClient restClient, Server server) {
    return login(restClient, server.getXuiLogin(), server.getXuiPassword());
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
    MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
    formData.add("username", username);
    formData.add("password", password);

    ResponseEntity<Void> response = restClient.post()
        .uri("/login")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(formData)
        .retrieve()
        .toBodilessEntity();

    String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    if (cookie == null) {
      throw new XuiUnavailableException("Login failed: No session cookie");
    }
    return cookie;
  }
}

