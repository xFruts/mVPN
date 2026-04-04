package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XuiPanelServiceImpl implements XuiPanelService {
  RestClient.Builder restClientBuilder;
  ObjectMapper objectMapper;

  @Override
  public String getVlessConfig(Server server, User user) {
    String baseUrl = String.format("https://%s:%d", server.getIp(), server.getPort());
    if (StringUtils.hasText(server.getWebBasePath())) {
      baseUrl += "/" + server.getWebBasePath();
    }
    RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();

    try {
      String sessionCookie = login(restClient, server.getXuiLogin(), server.getXuiPassword());
      XuiInboundsResponse inbounds = getInbounds(restClient, sessionCookie);
      return findVlessConfigInInbounds(inbounds, user.getFullName(), server.getIp());
    } catch (NotFoundException e) {
      log.warn("Config for user {} not found on server {}. Creating client and retrying once.",
          user.getFullName(), server.getName());
    } catch (XuiUnavailableException e) {
      throw e;
    }
    catch (Exception e) {
      // TODO: Добавить проверку на верность x-ui пароля, чтобы возвращалась отдельная ошибка.
      //  тут 404 не очень подходит как будто
      throw new XuiUnavailableException(
          "Unexpected error while getting config from XUI server: " + server.getName(), e);
    }
    try {
      createClient(server, user);

      String retryCookie = login(restClient, server.getXuiLogin(), server.getXuiPassword());
      XuiInboundsResponse retryInbounds = getInbounds(restClient, retryCookie);

      return findVlessConfigInInbounds(retryInbounds, user.getFullName(), server.getIp());
    } catch (NotFoundException e) {
      throw new NotFoundException(String.format("Config for user %s not found on server with id %d",
          user.getFullName(), server.getId()));
    } catch (XuiUnavailableException e) {
      throw e;
    } catch (Exception e) {
      throw new XuiUnavailableException(
          "Unexpected error while creating/retrying config on XUI server: " + server.getName(), e);
    }
  }

  @Override
  public void createClient(Server server, User user) {
    String baseUrl = String.format("https://%s:%d", server.getIp(), server.getPort());
    if (StringUtils.hasText(server.getWebBasePath())) {
      baseUrl += "/" + server.getWebBasePath();
    }
    RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();

    String sessionCookie = login(restClient, server.getXuiLogin(), server.getXuiPassword());
    XuiInboundsResponse inbounds = getInbounds(restClient, sessionCookie);

    Optional<XuiInboundsResponse.Inbound> vlessInboundOpt = inbounds.getObj().stream()
        .filter(inbound -> "vless".equalsIgnoreCase(inbound.getProtocol()))
        .findFirst();

    if (vlessInboundOpt.isEmpty()) {
      throw new XuiUnavailableException("No VLESS inbound found on server: " + server.getName());
    }

    addClientToInbound(restClient, sessionCookie, vlessInboundOpt.get(), user);
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

  private XuiInboundsResponse getInbounds(RestClient restClient, String sessionCookie) {
    return restClient.get()
        .uri("/panel/api/inbounds/list")
        .header(HttpHeaders.COOKIE, sessionCookie)
        .retrieve()
        .body(XuiInboundsResponse.class);
  }

  private void addClientToInbound(
      RestClient restClient, String sessionCookie, XuiInboundsResponse.Inbound inbound, User user) {
    try {
      ObjectNode newClientNode = objectMapper.createObjectNode();
      newClientNode.put("id", String.valueOf(user.getXuiId()));
      newClientNode.put("flow", "xtls-rprx-vision");
      newClientNode.put("email", user.getFullName());
      newClientNode.put("limitIp", 0);
      newClientNode.put("totalGB", 322122547200L);
      newClientNode.put("expiryTime",
          Instant.now().atZone(ZoneId.systemDefault()).plusMonths(1).toInstant().toEpochMilli());
      newClientNode.put("enable", true);

      if (user.getUserTelegramId() != null) {
        newClientNode.put("tgId", user.getUserTelegramId().toString());
      } else {
        newClientNode.put("tgId", "");
      }
      newClientNode.put("subId", String.valueOf(user.getXuiSubscription()));
      newClientNode.put("comment", "");
      newClientNode.put("reset", 0);

      ObjectNode clientsWrapper = objectMapper.createObjectNode();
      clientsWrapper.set("clients", objectMapper.createArrayNode().add(newClientNode));

      String clientSettings = objectMapper.writeValueAsString(clientsWrapper);

      MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
      formData.add("id", String.valueOf(inbound.getId()));
      formData.add("settings", clientSettings);

      String response = restClient.post()
          .uri("/panel/api/inbounds/addClient")
          .header(HttpHeaders.COOKIE, sessionCookie)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(formData)
          .retrieve()
          .body(String.class);

      log.info("Successfully added client {} to inbound {} on server {}. Response: {}",
          user.getFullName(), inbound.getId(), "serverName", response);
    } catch (JsonProcessingException e) {
      log.error("Failed to prepare new client settings", e);
      throw new XuiUnavailableException("Failed to prepare new client settings");
    }
  }

  private String findVlessConfigInInbounds(
      XuiInboundsResponse inboundsResponse, String userEmail, String serverIp) {
    if (inboundsResponse == null
        || !inboundsResponse.isSuccess()
        || inboundsResponse.getObj() == null) {
      throw new XuiUnavailableException("Failed to get inbounds or inbounds list is empty");
    }

    return inboundsResponse.getObj().stream()
        .filter(inbound -> "vless".equalsIgnoreCase(inbound.getProtocol()))
        .flatMap(inbound -> findClientInInbound(inbound, userEmail)
            .stream()
            .map(client -> Pair.of(inbound, client)))
        .map(pair ->
            generateVlessLink(pair.getSecond(), serverIp, pair.getFirst()))
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> new NotFoundException("VLESS config for user " + userEmail));
  }

  private Optional<XuiClient> findClientInInbound(
      XuiInboundsResponse.Inbound inbound, String userEmail) {
    try {
      JsonNode settings = objectMapper.readTree(inbound.getSettings());
      JsonNode clients = settings.get("clients");
      if (clients != null && clients.isArray()) {
        for (JsonNode clientNode : clients) {
          XuiClient client = objectMapper.treeToValue(clientNode, XuiClient.class);
          if (userEmail.equals(client.getEmail())) {
            return Optional.of(client);
          }
        }
      }
    } catch (JsonProcessingException e) {
      throw new XuiUnavailableException("Failed to parse inbound settings", e);
    }
    return Optional.empty();
  }

  private String generateVlessLink(
      XuiClient client, String address, XuiInboundsResponse.Inbound inbound) {
    try {
      JsonNode settings = objectMapper.readTree(inbound.getSettings());
      JsonNode streamSettings = objectMapper.readTree(inbound.getStreamSettings());

      JsonNode clientSettings = null;
      for (JsonNode c : settings.get("clients")) {
        if (client.getEmail().equals(c.get("email").asText())) {
          clientSettings = c;
          break;
        }
      }

      if (clientSettings == null || !clientSettings.get("enable").asBoolean()) {
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
      String finalRemark = URLEncoder.encode(remark, StandardCharsets.UTF_8);

      return String.format("vless://%s@%s:%d?%s#%s",
          uuid, address, inbound.getPort(), queryParams, finalRemark);

    } catch (Exception e) {
      log.error("Unexpected error generating VLESS link for inbound ID: {}", inbound.getId(), e);
      throw new XuiUnavailableException("Failed to generate VLESS link due to unexpected error");
    }
  }
}
