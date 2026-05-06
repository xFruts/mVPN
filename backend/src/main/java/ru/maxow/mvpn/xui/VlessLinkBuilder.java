package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
class VlessLinkBuilder {
  private final ObjectMapper objectMapper;

  VlessLinkBuilder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Формирует VLESS-ссылку на основе настроек inbound и streamSettings.
   * Возвращает null, если клиент отключен или просрочен.
   */
  String generateVlessLink(
      XuiClient client,
      String address,
      String countryEmoji,
      XuiInboundsResponse.Inbound inbound) {
    try {
      JsonNode settings = objectMapper.readTree(inbound.getSettings());
      JsonNode streamSettings = objectMapper.readTree(inbound.getStreamSettings());

      JsonNode clientSettings = null;
      for (JsonNode c : settings.get("clients")) {
        if (client.getId().equals(c.get("id").asText())) {
          clientSettings = c;
          break;
        }
      }

      if (clientSettings == null || !clientSettings.get("enable").asBoolean()) {
        log.info("XUI client is disabled or missing in inbound settings. inboundId={}, xuiId={}",
            inbound.getId(), client.getId());
        return null;
      }

      if (clientSettings.has("expiryTime")) {
        long expiryTime = clientSettings.get("expiryTime").asLong(0L);
        if (expiryTime > 0 && expiryTime < Instant.now().toEpochMilli()) {
          log.info("XUI client is expired. inboundId={}, xuiId={}, expiryTime={}",
              inbound.getId(), client.getId(), expiryTime);
          return null;
        }
      }

      if (!clientSettings.has("id") || !clientSettings.has("email")) {
        log.warn("XUI client payload has missing fields. inboundId={}, xuiId={}",
            inbound.getId(), client.getId());
        return null;
      }

      String uuid = clientSettings.get("id").asText();
      String flow = clientSettings.has("flow") ? clientSettings.get("flow").asText() : "";

      String network = streamSettings.get("network").asText();
      String security = streamSettings.get("security").asText();

      StringBuilder queryParams = new StringBuilder();
      queryParams.append("type=").append(network);
      queryParams.append("&encryption=none");

      if ("reality".equalsIgnoreCase(security)) {
        JsonNode realitySettings = streamSettings.get("realitySettings");
        if (realitySettings != null) {
          queryParams.append("&security=reality");

          JsonNode nestedRealitySettings = realitySettings.get("settings");
          if (nestedRealitySettings != null) {
            if (nestedRealitySettings.has("publicKey")) {
              queryParams.append("&pbk=").append(nestedRealitySettings.get("publicKey").asText());
            }
            if (nestedRealitySettings.has("fingerprint")) {
              queryParams.append("&fp=").append(nestedRealitySettings.get("fingerprint").asText());
            }
            if (nestedRealitySettings.has("spiderX")) {
              String spx = nestedRealitySettings.get("spiderX").asText();
              queryParams.append("&spx=").append(URLEncoder.encode(spx, StandardCharsets.UTF_8));
            }
          }

          if (realitySettings.has("serverNames")
              && !realitySettings.get("serverNames").isEmpty()) {
            queryParams.append("&sni=").append(realitySettings.get("serverNames").get(0).asText());
          }
          if (realitySettings.has("shortIds")
              && !realitySettings.get("shortIds").isEmpty()) {
            queryParams.append("&sid=").append(realitySettings.get("shortIds").get(0).asText());
          }
        }
      } else if ("tls".equalsIgnoreCase(security)) {
        JsonNode tlsSettings = streamSettings.get("tlsSettings");
        if (tlsSettings != null) {
          queryParams.append("&security=tls");
          if (tlsSettings.has("fingerprint")) {
            queryParams.append("&fp=").append(tlsSettings.get("fingerprint").asText());
          }
          if (tlsSettings.has("serverName")) {
            queryParams.append("&sni=").append(tlsSettings.get("serverName").asText());
          }
        }
      }
      switch (network) {
        case "ws":
          JsonNode wsSettings = streamSettings.get("wsSettings");
          if (wsSettings != null && wsSettings.has("path")
              && wsSettings.has("headers")) {
            String path = wsSettings.get("path").asText();
            String host = wsSettings.get("headers").get("Host").asText();
            queryParams.append("&path=").append(URLEncoder.encode(path, StandardCharsets.UTF_8));
            queryParams.append("&host=").append(URLEncoder.encode(host, StandardCharsets.UTF_8));
          }
          break;
        case "grpc":
          JsonNode grpcSettings = streamSettings.get("grpcSettings");
          if (grpcSettings != null && grpcSettings.has("serviceName")) {
            queryParams
                .append("&serviceName=")
                .append(URLEncoder.encode(
                    grpcSettings.get("serviceName").asText(), StandardCharsets.UTF_8));
          }
          break;
        default:
          break;
      }

      if (flow != null && !flow.isEmpty()) {
        queryParams.append("&flow=").append(flow);
      }

      String remark = inbound.getRemark() != null && !inbound.getRemark().isEmpty()
          ? inbound.getRemark() : inbound.getTag();
      if (countryEmoji != null && !countryEmoji.isEmpty()) {
        remark += countryEmoji;
      }
      String finalRemark = UriUtils.encodeFragment(remark, StandardCharsets.UTF_8);

      return String.format("vless://%s@%s:%d?%s#%s",
          uuid, address, inbound.getPort(), queryParams, finalRemark);

    } catch (Exception e) {
      log.error("Unexpected error generating VLESS link for inbound ID: {}", inbound.getId(), e);
      throw new XuiUnavailableException("Failed to generate VLESS link due to unexpected error");
    }
  }
}

