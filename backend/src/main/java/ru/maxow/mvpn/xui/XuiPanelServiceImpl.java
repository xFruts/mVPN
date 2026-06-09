package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.dto.XuiClientDto;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Реализация интеграции с 3X-UI Panel API v3.
 * <p>
 * Ключевые отличия от предыдущей версии:
 * <ul>
 *   <li>Клиенты управляются через {@code /panel/api/clients/*} как самостоятельные сущности</li>
 *   <li>Нет зависимости от {@code SubscriptionService} — подписка передаётся снаружи</li>
 *   <li>Нет ручной мутации {@code settings.clients[]} — это делает сервер при вызове clients API</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class XuiPanelServiceImpl implements XuiPanelService {

  private static final long BYTES_IN_GIGABYTE = 1024L * 1024L * 1024L;
  private static final String PROTOCOL_VLESS = "vless";
  private static final String SETTINGS_CLIENTS_NODE = "clients";
  private static final String DEFAULT_FLOW = "xtls-rprx-vision";

  XuiSessionClient sessionClient;
  XuiClientApiClient clientApiClient;
  XuiInboundClient inboundClient;
  XuiJsonConfigClient jsonConfigClient;
  VlessLinkBuilder vlessLinkBuilder;


  @Override
  public String getVlessConfig(Server server, User user) {
    RestClient restClient = sessionClient.buildPanelClient(server);

    try {
      String sessionCookie = sessionClient.login(restClient, server);
      XuiInboundsResponse inbounds = inboundClient.getInbounds(restClient, sessionCookie);

      return findVlessConfigInInbounds(inbounds, user, server.getIp(), server.getCountryEmoji());
    } catch (NotFoundException e) {
      throw new NotFoundException(String.format(
          "Config for user %s not found on server with id %d",
          user.getFullName(), server.getId()));
    } catch (XuiUnavailableException e) {
      throw e;
    } catch (Exception e) {
      throw new XuiUnavailableException(
          "Unexpected error while getting VLESS config from XUI server: " + server.getName(), e);
    }
  }

  @Override
  public String getJsonConfig(Server server, User user) {
    RestClient panelRestClient = sessionClient.buildPanelClient(server);
    RestClient jsonRestClient = sessionClient.buildRootClient(server);

    try {
      String sessionCookie = sessionClient.login(panelRestClient, server);

      if (user.getXuiSubscription() == null) {
        throw new XuiUnavailableException("Missing XUI subscription id for user: " + user.getId());
      }

      int subscriptionPort = jsonConfigClient.resolveSubscriptionPort(
          panelRestClient, sessionCookie, server);

      String rootJsonPath = jsonConfigClient.buildJsonSubscriptionUrl(server, user, subscriptionPort);
      String jsonConfig = jsonConfigClient.fetchJsonConfigAtPath(
          jsonRestClient, sessionCookie, rootJsonPath, server);

      return jsonConfigClient.replaceRemarksWithServerName(jsonConfig, server);
    } catch (XuiUnavailableException e) {
      throw e;
    } catch (RestClientException e) {
      log.warn("Failed to get JSON config from XUI server. serverId={}, userId={}, xuiId={}",
          server.getId(), user.getId(), user.getXuiId(), e);
      throw new XuiUnavailableException(
          "Failed to get JSON config from XUI server: " + server.getName(), e);
    } catch (Exception e) {
      log.error("Unexpected error while getting JSON config. serverId={}, userId={}",
          server.getId(), user.getId(), e);
      throw new XuiUnavailableException(
          "Unexpected error while getting JSON config from XUI server: " + server.getName(), e);
    }
  }

  @Override
  public void createOrUpdateClient(Server server, User user, Subscription subscription) {
    RestClient restClient = sessionClient.buildPanelClient(server);

    try {
      String sessionCookie = sessionClient.login(restClient, server);
      upsertClient(restClient, sessionCookie, server, user, subscription);
    } catch (XuiUnavailableException e) {
      throw e;
    } catch (Exception e) {
      log.error("Failed to create/update client on XUI. serverId={}, userId={}",
          server.getId(), user.getId(), e);
      throw new XuiUnavailableException(
          "Failed to create/update client on server: " + server.getName(), e);
    }
  }

  @Override
  public XuiClientTraffic getClientTraffic(Server server, String clientEmail) {
    RestClient restClient = sessionClient.buildPanelClient(server);

    try {
      String sessionCookie = sessionClient.login(restClient, server);
      XuiClientDto client = clientApiClient.getClient(restClient, sessionCookie, clientEmail);

      if (client == null) {
        log.warn("Client not found on XUI for traffic query. serverId={}, email={}",
            server.getId(), clientEmail);
        return XuiClientTraffic.zero(clientEmail);
      }

      XuiClientDto.TrafficInfo traffic = client.traffic();

      long expiryTime = client.expiryTime() != null ? client.expiryTime() : 0L;
      if (traffic == null) {
        return new XuiClientTraffic(clientEmail, 0L, 0L, 0L, expiryTime);
      }

      long up = traffic.up() != null ? traffic.up() : 0L;
      long down = traffic.down() != null ? traffic.down() : 0L;
      long total = up + down;

      return new XuiClientTraffic(clientEmail, up, down, total, expiryTime);

    } catch (XuiUnavailableException e) {
      throw e;
    } catch (RestClientException e) {
      log.warn("Failed to get traffic from XUI. serverId={}, email={}",
          server.getId(), clientEmail, e);
      throw new XuiUnavailableException(
          "Failed to get traffic from XUI server: " + server.getName(), e);
    } catch (Exception e) {
      log.error("Unexpected error getting traffic. serverId={}, email={}",
          server.getId(), clientEmail, e);
      throw new XuiUnavailableException(
          "Unexpected error while getting traffic: " + server.getName(), e);
    }
  }

  /**
   * Upsert клиента: проверяет существование через GET, затем add или update.
   */
  private void upsertClient(RestClient restClient, String sessionCookie,
                             Server server, User user, Subscription subscription) {
    String clientEmail = user.getFullName();
    Tariff tariff = subscription.getTariff();

    XuiInboundsResponse inbounds = inboundClient.getInbounds(restClient, sessionCookie);
    List<Integer> vlessInboundIds = findVlessInboundIds(inbounds);

    if (vlessInboundIds.isEmpty()) {
      throw new XuiUnavailableException("No VLESS inbound found on server: " + server.getName());
    }

    XuiClientDto existingClient = clientApiClient.getClient(restClient, sessionCookie, clientEmail);
    XuiClientDto clientDto = buildClientDto(user, subscription, tariff, existingClient);

    if (existingClient != null) {
      clientApiClient.updateClient(restClient, sessionCookie, clientEmail, clientDto, server);
    } else {
      clientApiClient.addClient(restClient, sessionCookie, clientDto, vlessInboundIds, server);
    }
  }

  /**
   * Собирает типизированный DTO клиента из доменных объектов.
   */
  private XuiClientDto buildClientDto(User user, Subscription subscription,
                                       Tariff tariff, XuiClientDto existing) {
    return new XuiClientDto(
        String.valueOf(user.getXuiId()),
        user.getFullName(),
        String.valueOf(user.getXuiSubscription()),
        tariff.getTrafficLimitGb() * BYTES_IN_GIGABYTE,
        subscription.getEndDate().toInstant().toEpochMilli(),
        true,
        null,
        tariff.getMaxDevices(),
        existing != null ? existing.flow() : DEFAULT_FLOW ,
        existing != null ? existing.password() : null,
        existing != null ? existing.security() : null,
        null,
        null,
        null
    );
  }

  /**
   * Ищет VLESS-конфиг в списке inbound'ов для пользователя.
   * Клиент ищется в {@code settings.clients[]} по UUID (xuiId).
   */
  private String findVlessConfigInInbounds(
      XuiInboundsResponse inboundsResponse,
      User user,
      String serverIp,
      String countryEmoji) {

    if (inboundsResponse == null || !inboundsResponse.success() || inboundsResponse.obj() == null) {
      throw new XuiUnavailableException("Failed to get inbounds or inbounds list is empty");
    }

    String expectedUuid = String.valueOf(user.getXuiId());

    return inboundsResponse.obj().stream()
        .filter(inbound -> PROTOCOL_VLESS.equalsIgnoreCase(inbound.protocol()))
        .flatMap(inbound -> findClientNodeInSettings(inbound, expectedUuid, user.getFullName())
            .stream()
            .map(clientNode -> Pair.of(inbound, clientNode)))
        .map(pair -> vlessLinkBuilder.generateVlessLink(
            pair.getSecond(), serverIp, countryEmoji, pair.getFirst()))
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> new NotFoundException(
            String.format("VLESS config for user %s not found on server", user.getFullName())));
  }

  /**
   * Ищет клиента в {@code settings.clients[]} inbound'а по UUID или email (fallback).
   */
  private Optional<tools.jackson.databind.JsonNode> findClientNodeInSettings(
      XuiInboundsResponse.Inbound inbound, String expectedUuid, String expectedEmail) {

    var settings = inbound.settings();
    if (settings == null) {
      return Optional.empty();
    }

    var clients = settings.get(SETTINGS_CLIENTS_NODE);
    if (clients == null || !clients.isArray()) {
      return Optional.empty();
    }

    JsonNode fallback = null;
    for (var clientNode : clients) {
      String nodeId = clientNode.path("id").asString("");
      if (expectedUuid.equals(nodeId)) {
        return Optional.of(clientNode);
      }
      if (expectedEmail.equals(clientNode.path("email").asString(""))) {
        fallback = clientNode;
      }
    }

    if (fallback != null) {
      log.warn("XUI client matched by email fallback. inboundId={}, uuid={}, email={}",
          inbound.id(), expectedUuid, expectedEmail);
    }
    return Optional.ofNullable(fallback);
  }

  /**
   * Возвращает ID всех VLESS inbound'ов.
   */
  private List<Integer> findVlessInboundIds(XuiInboundsResponse inbounds) {
    if (inbounds == null || inbounds.obj() == null) {
      return List.of();
    }

    return inbounds.obj().stream()
        .filter(inbound -> PROTOCOL_VLESS.equalsIgnoreCase(inbound.protocol()))
        .map(XuiInboundsResponse.Inbound::id)
        .toList();
  }
}
