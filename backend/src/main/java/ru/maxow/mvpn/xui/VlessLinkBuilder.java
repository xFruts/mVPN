package ru.maxow.mvpn.xui;

import tools.jackson.databind.JsonNode;
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

  /**
   * Формирует VLESS-ссылку на основе настроек inbound и streamSettings.
   * Возвращает null, если клиент отключен или просрочен.
   */
  String generateVlessLink(
      JsonNode clientSettings,
      String address,
      String countryEmoji,
      XuiInboundsResponse.Inbound inbound) {
    try {
      JsonNode streamSettings = inbound.streamSettings();

      if (clientSettings == null || !clientSettings.get("enable").asBoolean(false)) {
        log.info("XUI client is disabled or missing in inbound settings. inboundId={}, clientId={}",
            inbound.id(), clientSettings != null ? clientSettings.path("id").asString() : "null");
        return null;
      }

      if (clientSettings.has("expiryTime")) {
        long expiryTime = clientSettings.get("expiryTime").asLong(0L);
        if (expiryTime > 0 && expiryTime < Instant.now().toEpochMilli()) {
          log.info("XUI client is expired. inboundId={}, clientId={}, expiryTime={}",
              inbound.id(), clientSettings.path("id").asString(), expiryTime);
          return null;
        }
      }

      if (!clientSettings.has("id") || !clientSettings.has("email")) {
        log.warn("XUI client payload has missing fields. inboundId={}", inbound.id());
        return null;
      }

      String uuid = clientSettings.get("id").asString();
      String flow = clientSettings.has("flow") ? clientSettings.get("flow").asString() : "";

      String network = streamSettings.get("network").asString();
      String security = streamSettings.get("security").asString();

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
              queryParams.append("&pbk=").append(nestedRealitySettings.get("publicKey").asString());
            }
            if (nestedRealitySettings.has("fingerprint")) {
              queryParams.append("&fp=").append(nestedRealitySettings.get("fingerprint").asString());
            }
            if (nestedRealitySettings.has("spiderX")) {
              String spx = nestedRealitySettings.get("spiderX").asString();
              queryParams.append("&spx=").append(URLEncoder.encode(spx, StandardCharsets.UTF_8));
            }
          }

          if (realitySettings.has("serverNames")
              && !realitySettings.get("serverNames").isEmpty()) {
            queryParams.append("&sni=").append(realitySettings.get("serverNames").get(0).asString());
          }
          if (realitySettings.has("shortIds")
              && !realitySettings.get("shortIds").isEmpty()) {
            queryParams.append("&sid=").append(realitySettings.get("shortIds").get(0).asString());
          }
        }
      } else if ("tls".equalsIgnoreCase(security)) {
        JsonNode tlsSettings = streamSettings.get("tlsSettings");
        if (tlsSettings != null) {
          queryParams.append("&security=tls");
          if (tlsSettings.has("fingerprint")) {
            queryParams.append("&fp=").append(tlsSettings.get("fingerprint").asString());
          }
          if (tlsSettings.has("serverName")) {
            queryParams.append("&sni=").append(tlsSettings.get("serverName").asString());
          }
        }
      }
      switch (network) {
        case "ws":
          JsonNode wsSettings = streamSettings.get("wsSettings");
          if (wsSettings != null && wsSettings.has("path")
              && wsSettings.has("headers")) {
            String path = wsSettings.get("path").asString();
            String host = wsSettings.get("headers").get("Host").asString();
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
                    grpcSettings.get("serviceName").asString(), StandardCharsets.UTF_8));
          }
          break;
        default:
          break;
      }

      if (flow != null && !flow.isEmpty()) {
        queryParams.append("&flow=").append(flow);
      }

      String remark = inbound.remark() != null && !inbound.remark().isEmpty()
          ? inbound.remark() : inbound.tag();
      if (countryEmoji != null && !countryEmoji.isEmpty()) {
        remark += countryEmoji;
      }
      String finalRemark = UriUtils.encodeFragment(remark, StandardCharsets.UTF_8);

      return String.format("vless://%s@%s:%d?%s#%s",
          uuid, address, inbound.port(), queryParams, finalRemark);

    } catch (Exception e) {
      log.error("Unexpected error generating VLESS link for inbound ID: {}", inbound.id(), e);
      throw new XuiUnavailableException("Failed to generate VLESS link due to unexpected error");
    }
  }
}
