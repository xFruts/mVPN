package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.user.User;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class XuiPanelServiceImpl implements XuiPanelService {
  WebClient.Builder webClientBuilder;
  ObjectMapper objectMapper;

  @Override
  public Mono<String> getVlessConfig(Server server, User user) {
    String baseUrl = String.format("http://%s:%d", server.getIp(), server.getPort());
    if (StringUtils.hasText(server.getWebBasePath())) {
      baseUrl += "/" + server.getWebBasePath();
    }
    log.info("baseUrl: {}", baseUrl);
    WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

    return login(webClient, server.getXuiLogin(), server.getPassword())
        .flatMap(sessionCookie -> getInbounds(webClient, sessionCookie)
            .flatMap(inbounds ->
                findVlessConfigInInbounds(inbounds, user.getFullName(), server.getIp())))
        .onErrorResume(e -> {
          if (e.getMessage()
              .contains("VLESS config for user " + user.getFullName() + " not found")) {
            log.warn("Config for user {} not found on server {}. Creating a new one.",
                user.getFullName(), server.getName());
            return createClient(server, user)
                .then(getVlessConfig(server, user));
          }
          return Mono.error(e);
        });
  }

  @Override
  public Mono<Void> createClient(Server server, User user) {
    String baseUrl = String.format("http://%s:%d", server.getIp(), server.getPort());
    if (StringUtils.hasText(server.getWebBasePath())) {
      baseUrl += "/" + server.getWebBasePath();
    }
    WebClient webClient = webClientBuilder.baseUrl(baseUrl).build();

    return login(webClient, server.getXuiLogin(), server.getPassword())
        .flatMap(sessionCookie -> getInbounds(webClient, sessionCookie)
            .flatMap(inbounds -> {
              Optional<XuiInboundsResponse.Inbound> vlessInboundOpt = inbounds.getObj().stream()
                  .filter(inbound -> "vless".equalsIgnoreCase(inbound.getProtocol()))
                  .findFirst();

              if (vlessInboundOpt.isEmpty()) {
                return Mono.error(
                    new RuntimeException("No VLESS inbound found on server " + server.getName()));
              }

              return addClientToInbound(webClient, sessionCookie, vlessInboundOpt.get(), user);
            })
        ).then();
  }

  private Mono<String> login(WebClient webClient, String username, String password) {
    log.info("username: {}, password: {}", username, password);
    return webClient.post()
        .uri("/login")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData("username", username).with("password", password))
        .retrieve()
        .toBodilessEntity()
        .map(response ->
            Optional.ofNullable(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)))
        .flatMap(cookieOpt -> cookieOpt.map(Mono::just)
            .orElseGet(() -> Mono.error(new RuntimeException("Login failed: No session cookie"))));
  }

  private Mono<XuiInboundsResponse> getInbounds(WebClient webClient, String sessionCookie) {
    return webClient.get()
        .uri("/panel/api/inbounds/list")
        .header(HttpHeaders.COOKIE, sessionCookie)
        .retrieve()
        .bodyToMono(XuiInboundsResponse.class);
  }

  private Mono<Void> addClientToInbound(
      WebClient webClient, String sessionCookie, XuiInboundsResponse.Inbound inbound, User user) {
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

      return webClient.post()
          .uri("/panel/api/inbounds/addClient")
          .header(HttpHeaders.COOKIE, sessionCookie)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(BodyInserters.fromFormData("id", String.valueOf(inbound.getId()))
              .with("settings", clientSettings))
          .retrieve()
          .bodyToMono(String.class)
          .doOnSuccess(response ->
              log.info("Successfully added client {} to inbound {} on server {}. Response: {}",
                  user.getFullName(), inbound.getId(), "serverName", response))
          .then();
    } catch (JsonProcessingException e) {
      return Mono.error(new RuntimeException("Failed to prepare new client settings", e));
    }
  }

  private Mono<String> findVlessConfigInInbounds(
      XuiInboundsResponse inboundsResponse, String userEmail, String serverIp) {
    if (inboundsResponse == null
        || !inboundsResponse.isSuccess()
        || inboundsResponse.getObj() == null) {
      return Mono.error(new RuntimeException("Failed to get inbounds or inbounds list is empty"));
    }

    return Mono.justOrEmpty(inboundsResponse.getObj().stream()
            .filter(inbound -> "vless".equalsIgnoreCase(inbound.getProtocol()))
            .flatMap(inbound -> findClientInInbound(inbound, userEmail).stream())
            .map(client ->
                generateVlessLink(client, serverIp, inboundsResponse.getObj().getFirst()))
            .findFirst())
        .switchIfEmpty(
            Mono.error(new RuntimeException("VLESS config for user " + userEmail + " not found")));
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
      log.error("Error parsing inbound settings", e);
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
      }

      if (flow != null && !flow.isEmpty()) {
        queryParams.append("&flow=").append(flow);
      }

      String remark = inbound.getRemark() != null && !inbound.getRemark().isEmpty()
          ? inbound.getRemark() : inbound.getTag();
      String finalRemark = URLEncoder.encode(remark, StandardCharsets.UTF_8);

      String finalLink = String.format("vless://%s@%s:%d?%s#%s",
          uuid, address, inbound.getPort(), queryParams, finalRemark);

      log.info("Generated VLESS link: {}", finalLink);
      return finalLink;

    } catch (Exception e) {
      log.error("Unexpected error generating VLESS link for inbound ID: {}", inbound.getId(), e);
      throw new RuntimeException("Unexpected error", e);
    }
  }
}
