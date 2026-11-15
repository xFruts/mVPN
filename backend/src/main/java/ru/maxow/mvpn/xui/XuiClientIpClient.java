package ru.maxow.mvpn.xui;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.xui.dto.XuiApiResponse;
import ru.maxow.mvpn.xui.dto.XuiClientIpRecord;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * HTTP-клиент для чтения и сброса активных IP-адресов клиентов в 3X-UI.
 */
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class XuiClientIpClient {

  /**
   * Ссылка на обобщенный тип ответа API со списком IP-адресов клиента.
   */
  static ParameterizedTypeReference<XuiApiResponse<JsonNode>> IP_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};

  ObjectMapper objectMapper;

  @Autowired
  XuiClientIpClient(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
  }

  public List<XuiClientIpRecord> getClientIps(
      RestClient restClient,
      String sessionCookie,
      String email
  ) {
    String encodedEmail = UriUtils.encodePathSegment(email, StandardCharsets.UTF_8);
    XuiApiResponse<JsonNode> response = tryGetIps(
        restClient, sessionCookie, "/panel/api/inbounds/clientIps/" + encodedEmail
    );
    if (response == null || !response.success() || response.obj() == null) {
      response = tryGetIps(
          restClient, sessionCookie, "/panel/api/clients/ips/" + encodedEmail
      );
    }

    if (response == null || !response.success() || response.obj() == null) {
      return List.of();
    }

    return parseIpRecords(response.obj());
  }

  public void clearClientIps(
      RestClient restClient,
      String sessionCookie,
      String email,
      Server server
  ) {
    String encodedEmail = UriUtils.encodePathSegment(email, StandardCharsets.UTF_8);
    try {
      sendPost(restClient, sessionCookie, "/panel/api/inbounds/clearClientIps/" + encodedEmail);
    } catch (Exception e) {
      log.debug("clearClientIps on inbounds endpoint failed, trying clients endpoint: {}",
          e.getMessage());
      try {
        sendPost(restClient, sessionCookie, "/panel/api/clients/clearIps/" + encodedEmail);
      } catch (Exception ex) {
        log.warn("Failed to clear client IPs for {} in 3X-UI on {}: {}",
            email, server.getName(), ex.getMessage());
      }
    }
  }

  private XuiApiResponse<JsonNode> tryGetIps(
      RestClient restClient,
      String sessionCookie,
      String path
  ) {
    try {
      RestClient.RequestBodySpec request = restClient.post().uri(path);
      if (sessionCookie != null) {
        request = request.header(HttpHeaders.COOKIE, sessionCookie);
      }
      return request
          .contentType(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(IP_RESPONSE_TYPE);
    } catch (RestClientResponseException e) {
      log.debug("Endpoint {} returned error: {} - {}", path, e.getStatusCode(),
          e.getResponseBodyAsString());
      return null;
    } catch (Exception e) {
      log.debug("Request to {} failed: {}", path, e.getMessage());
      return null;
    }
  }

  private void sendPost(
      RestClient restClient,
      String sessionCookie,
      String path
  ) {
    RestClient.RequestBodySpec request = restClient.post().uri(path);
    if (sessionCookie != null) {
      request = request.header(HttpHeaders.COOKIE, sessionCookie);
    }
    request
        .contentType(MediaType.APPLICATION_JSON)
        .retrieve()
        .toBodilessEntity();
  }

  private List<XuiClientIpRecord> parseIpRecords(JsonNode node) {
    if (node == null || node.isNull()) {
      return List.of();
    }

    if (node.isString()) {
      String text = node.asString().trim();
      if (text.isEmpty() || "No IP Record".equalsIgnoreCase(text)) {
        return List.of();
      }
      try {
        JsonNode parsed = objectMapper.readTree(text);
        return parseArrayNode(parsed);
      } catch (Exception ignored) {
        String cleanIp = cleanIp(text);
        if (!cleanIp.isBlank()) {
          return List.of(new XuiClientIpRecord(cleanIp, null));
        }
        return List.of();
      }
    }

    return parseArrayNode(node);
  }

  private List<XuiClientIpRecord> parseArrayNode(JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }
    List<XuiClientIpRecord> result = new ArrayList<>();
    for (JsonNode item : node) {
      if (item.isString()) {
        String ip = cleanIp(item.asString());
        if (!ip.isBlank()) {
          result.add(new XuiClientIpRecord(ip, null));
        }
      } else if (item.isObject()) {
        String ip = null;
        if (item.has("ip")) {
          ip = cleanIp(item.get("ip").asString());
        } else if (item.has("clientIp")) {
          ip = cleanIp(item.get("clientIp").asString());
        } else if (item.has("addr")) {
          ip = cleanIp(item.get("addr").asString());
        }
        Long timestamp = null;
        if (item.has("timestamp") && item.get("timestamp").isNumber()) {
          timestamp = item.get("timestamp").asLong();
        } else if (item.has("time") && item.get("time").isNumber()) {
          timestamp = item.get("time").asLong();
        }
        if (ip != null && !ip.isBlank()) {
          result.add(new XuiClientIpRecord(ip, timestamp));
        }
      }
    }
    return result;
  }

  private String cleanIp(String raw) {
    if (raw == null) {
      return "";
    }
    String trimmed = raw.trim();
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
      trimmed = trimmed.substring(1, trimmed.length() - 1);
    }
    if (trimmed.startsWith("::ffff:")) {
      trimmed = trimmed.substring("::ffff:".length());
    }
    if (trimmed.contains(":") && trimmed.indexOf(':') == trimmed.lastIndexOf(':')) {
      trimmed = trimmed.substring(0, trimmed.indexOf(':'));
    }
    return trimmed;
  }
}
