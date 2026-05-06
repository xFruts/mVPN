package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
class XuiJsonConfigClient {
  private static final int DEFAULT_SUBSCRIPTION_PORT = 2096;

  private final ObjectMapper objectMapper;

  XuiJsonConfigClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  int resolveSubscriptionPort(RestClient panelRestClient, String sessionCookie, Server server) {
    try {
      String settingsBody = panelRestClient.post()
          .uri("/panel/setting/all")
          .header(HttpHeaders.COOKIE, sessionCookie)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(new LinkedMultiValueMap<String, String>())
          .retrieve()
          .body(String.class);

      JsonNode settingsResponse = null;
      if (settingsBody != null && !settingsBody.isBlank()) {
        settingsResponse = objectMapper.readTree(settingsBody);
      }

      int subPort = settingsResponse == null
          ? DEFAULT_SUBSCRIPTION_PORT
          : settingsResponse.path("obj").path("subPort").asInt(DEFAULT_SUBSCRIPTION_PORT);

      if (subPort <= 0) {
        log.warn("Invalid subPort from XUI settings. serverId={}, subPort={}, fallback={}",
            server.getId(), subPort, DEFAULT_SUBSCRIPTION_PORT);
        return DEFAULT_SUBSCRIPTION_PORT;
      }
      return subPort;
    } catch (Exception e) {
      log.warn("Failed to resolve subPort from XUI settings. serverId={}, fallback={}",
          server.getId(), DEFAULT_SUBSCRIPTION_PORT, e);
      return DEFAULT_SUBSCRIPTION_PORT;
    }
  }

  String buildJsonSubscriptionUrl(Server server, User user, int subscriptionPort) {
    String baseUrl = String.format("https://%s:%d", server.getIp(), subscriptionPort);
    String encodedSubId = UriUtils.encodePathSegment(
        user.getXuiSubscription().toString(), StandardCharsets.UTF_8);
    return baseUrl + "/json/" + encodedSubId;
  }

  String fetchJsonConfigAtPath(
      RestClient jsonRestClient,
      String sessionCookie,
      String jsonPath,
      Server server) {
    String response = jsonRestClient.get()
        .uri(jsonPath)
        .header(HttpHeaders.COOKIE, sessionCookie)
        .retrieve()
        .body(String.class);

    if (!StringUtils.hasText(response) || "Error!".equalsIgnoreCase(response.trim())) {
      throw new XuiUnavailableException(
          "XUI returned invalid JSON subscription response for server: " + server.getName());
    }

    return response;
  }

  String replaceRemarksWithServerName(String jsonConfig, Server server) {
    try {
      JsonNode root = objectMapper.readTree(jsonConfig);
      if (root.isObject()) {
        ((ObjectNode) root).put("remarks", server.getName() + server.getCountryEmoji());
        return objectMapper.writeValueAsString(root);
      }
      return jsonConfig;
    } catch (JsonProcessingException e) {
      log.warn("Failed to replace remarks in JSON config for server {}: {}",
          server.getId(), e.getMessage());
      return jsonConfig;
    }
  }
}
