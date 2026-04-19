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
import org.springframework.web.util.UriUtils;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XuiPanelServiceImpl implements XuiPanelService {
  private static final long BYTES_IN_GIGABYTE = 1024L * 1024L * 1024L;

  RestClient.Builder restClientBuilder;
  ObjectMapper objectMapper;
  SubscriptionService subscriptionService;

  @Override
  public String getVlessConfig(Server server, User user) {
    String baseUrl = buildBaseUrl(server);
    RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();

    try {
      String sessionCookie = login(restClient, server.getXuiLogin(), server.getXuiPassword());
      XuiInboundsResponse inbounds = getInbounds(restClient, sessionCookie);
      return findVlessConfigInInbounds(
          inbounds, user, server.getIp(), server.getCountryEmoji());
    } catch (NotFoundException e) {
      log.warn(
          "XUI config not found, upserting client and retrying once. serverId={}, serverName={},"
              + " userId={}, xuiId={}, userEmail={}",
          server.getId(),
          server.getName(),
          user.getId(),
          user.getXuiId(),
          user.getFullName());
    } catch (XuiUnavailableException e) {
      throw e;
    } catch (Exception e) {
      throw new XuiUnavailableException(
          "Unexpected error while getting config from XUI server: " + server.getName(), e);
    }

    try {
      upsertClient(server, user, restClient);

      String retryCookie = login(restClient, server.getXuiLogin(), server.getXuiPassword());
      XuiInboundsResponse retryInbounds = getInbounds(restClient, retryCookie);

      return findVlessConfigInInbounds(
          retryInbounds, user, server.getIp(), server.getCountryEmoji());
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
    String baseUrl = buildBaseUrl(server);
    RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
    upsertClient(server, user, restClient);
  }

  private void upsertClient(Server server, User user, RestClient restClient) {
    String sessionCookie = login(restClient, server.getXuiLogin(), server.getXuiPassword());
    XuiInboundsResponse inbounds = getInbounds(restClient, sessionCookie);

    Optional<XuiInboundsResponse.Inbound> vlessInboundOpt = findVlessInbound(inbounds);

    if (vlessInboundOpt.isEmpty()) {
      throw new XuiUnavailableException("No VLESS inbound found on server: " + server.getName());
    }

    XuiInboundsResponse.Inbound inbound = vlessInboundOpt.get();
    Optional<XuiClient> existingClient = findClientInInbound(inbound, user);
    if (existingClient.isPresent()) {
      updateClientInInbound(restClient, sessionCookie, inbound, user, server, existingClient.get());
      return;
    }

    addClientToInbound(restClient, sessionCookie, inbound, user, server);
  }

  private String buildBaseUrl(Server server) {
    String baseUrl = String.format("https://%s:%d", server.getIp(), server.getPort());
    if (StringUtils.hasText(server.getWebBasePath())) {
      baseUrl += "/" + server.getWebBasePath();
    }
    return baseUrl;
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
      RestClient restClient,
      String sessionCookie,
      XuiInboundsResponse.Inbound inbound,
      User user,
      Server server) {
    try {
      Subscription subscription = subscriptionService.findLastSubscriptionEntityByUserId(user.getId());
      Tariff tariff = subscription.getTariff();

      String clientSettings = wrapClientPayload(
          buildClientPayload(user, subscription, tariff, null));

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

      log.info(
          "XUI addClient success. serverId={}, serverName={}, inboundId={}, userId={},"
              + " xuiId={}, response={}",
          server.getId(),
          server.getName(),
          inbound.getId(),
          user.getId(),
          user.getXuiId(),
          response);
    } catch (JsonProcessingException e) {
      log.error(
          "XUI addClient payload serialization failed. serverId={}, userId={}, xuiId={}",
          server.getId(),
          user.getId(),
          user.getXuiId(),
          e);
      throw new XuiUnavailableException("Failed to prepare new client settings");
    }
  }

  private void updateClientInInbound(
      RestClient restClient,
      String sessionCookie,
      XuiInboundsResponse.Inbound inbound,
      User user,
      Server server,
      XuiClient existingClient) {
    try {
      Subscription subscription = subscriptionService.findLastSubscriptionEntityByUserId(user.getId());
      Tariff tariff = subscription.getTariff();

      String clientSettings = wrapClientPayload(
          buildClientPayload(user, subscription, tariff, existingClient));

      MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
      formData.add("id", String.valueOf(inbound.getId()));
      formData.add("settings", clientSettings);

      String response = restClient.post()
          .uri("/panel/api/inbounds/updateClient/" + existingClient.getId())
          .header(HttpHeaders.COOKIE, sessionCookie)
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
    } catch (JsonProcessingException e) {
      log.error(
          "XUI updateClient payload serialization failed. serverId={}, userId={}, xuiId={}",
          server.getId(),
          user.getId(),
          user.getXuiId(),
          e);
      throw new XuiUnavailableException("Failed to edit client settings");
    }
  }

  private ObjectNode buildClientPayload(
      User user,
      Subscription subscription,
      Tariff tariff,
      XuiClient existingClient) {
    ObjectNode clientNode = objectMapper.createObjectNode();
    String flow = existingClient != null && StringUtils.hasText(existingClient.getFlow())
        ? existingClient.getFlow()
        : "xtls-rprx-vision";
    String comment = existingClient != null && existingClient.getComment() != null
        ? existingClient.getComment()
        : "";
    int reset = existingClient != null && existingClient.getReset() != null
        ? existingClient.getReset()
        : 0;

    clientNode.put("id", String.valueOf(user.getXuiId()));
    clientNode.put("flow", flow);
    clientNode.put("email", user.getFullName());
    clientNode.put("limitIp", tariff.getMaxDevices());

    // 3x-ui ожидает лимит трафика в байтах
    clientNode.put("totalGB", tariff.getTrafficLimitGb() * BYTES_IN_GIGABYTE);
    clientNode.put("expiryTime", subscription.getEndDate().toInstant().toEpochMilli());
    clientNode.put("enable", true);
    clientNode.put("subId", String.valueOf(user.getXuiSubscription()));
    clientNode.put("comment", comment);
    clientNode.put("reset", reset);
    return clientNode;
  }

  private String wrapClientPayload(ObjectNode clientNode) throws JsonProcessingException {
    ObjectNode clientsWrapper = objectMapper.createObjectNode();
    clientsWrapper.set("clients", objectMapper.createArrayNode().add(clientNode));
    return objectMapper.writeValueAsString(clientsWrapper);
  }

  private String findVlessConfigInInbounds(
      XuiInboundsResponse inboundsResponse,
      User user,
      String serverIp,
      String countryEmoji) {
    if (inboundsResponse == null
        || !inboundsResponse.isSuccess()
        || inboundsResponse.getObj() == null) {
      throw new XuiUnavailableException("Failed to get inbounds or inbounds list is empty");
    }

    return inboundsResponse.getObj().stream()
        .filter(inbound -> "vless".equalsIgnoreCase(inbound.getProtocol()))
        .flatMap(inbound -> findClientInInbound(inbound, user)
            .stream()
            .map(client -> Pair.of(inbound, client)))
        .map(pair ->
            generateVlessLink(pair.getSecond(), serverIp, countryEmoji, pair.getFirst()))
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> new NotFoundException("VLESS config for user " + user.getFullName()));
  }

  private Optional<XuiInboundsResponse.Inbound> findVlessInbound(XuiInboundsResponse inbounds) {
    if (inbounds == null || inbounds.getObj() == null) {
      return Optional.empty();
    }
    return inbounds.getObj().stream()
        .filter(inbound -> "vless".equalsIgnoreCase(inbound.getProtocol()))
        .findFirst();
  }

  private Optional<XuiClient> findClientInInbound(XuiInboundsResponse.Inbound inbound, User user) {
    try {
      JsonNode settings = objectMapper.readTree(inbound.getSettings());
      JsonNode clients = settings.get("clients");
      if (clients != null && clients.isArray()) {
        String expectedXuiId = String.valueOf(user.getXuiId());
        Optional<XuiClient> byEmail = Optional.empty();
        for (JsonNode clientNode : clients) {
          XuiClient client = objectMapper.treeToValue(clientNode, XuiClient.class);
          if (expectedXuiId.equals(client.getId())) {
            return Optional.of(client);
          }
          if (user.getFullName().equals(client.getEmail())) {
            byEmail = Optional.of(client);
          }
        }
        if (byEmail.isPresent()) {
          log.warn(
              "XUI client matched by email fallback. inboundId={}, userId={}, xuiId={}, email={}",
              inbound.getId(),
              user.getId(),
              user.getXuiId(),
              user.getFullName());
          return byEmail;
        }
      }
    } catch (JsonProcessingException e) {
      throw new XuiUnavailableException("Failed to parse inbound settings", e);
    }
    return Optional.empty();
  }

  private String generateVlessLink(
      XuiClient client, String address, String countryEmoji, XuiInboundsResponse.Inbound inbound) {
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
