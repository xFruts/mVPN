package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.web.context.request.RequestContextHolder;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionService;
import org.springframework.context.annotation.Lazy;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Интеграция с XUI (3x-ui): синхронизация клиентов, генерация конфигов и чтение трафика.
 */
@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class XuiPanelServiceImpl implements XuiPanelService {
  SubscriptionService subscriptionService;
  ObjectProvider<RequestScopedSubPortCache> subPortCacheProvider;
  XuiSessionClient sessionClient;
  XuiInboundClient inboundClient;
  XuiJsonConfigClient jsonConfigClient;
  XuiClientPayloadBuilder payloadBuilder;
  XuiInboundMutator inboundMutator;
  VlessLinkBuilder vlessLinkBuilder;
  XuiTrafficMapper trafficMapper;

  public XuiPanelServiceImpl(
      XuiSessionClient sessionClient,
      XuiInboundClient inboundClient,
      XuiJsonConfigClient jsonConfigClient,
      @Lazy SubscriptionService subscriptionService,
      ObjectProvider<RequestScopedSubPortCache> subPortCacheProvider,
      XuiClientPayloadBuilder payloadBuilder,
      XuiInboundMutator inboundMutator,
      VlessLinkBuilder vlessLinkBuilder,
      XuiTrafficMapper trafficMapper) {
    this.sessionClient = sessionClient;
    this.inboundClient = inboundClient;
    this.jsonConfigClient = jsonConfigClient;
    this.subscriptionService = subscriptionService;
    this.subPortCacheProvider = subPortCacheProvider;
    this.payloadBuilder = payloadBuilder;
    this.inboundMutator = inboundMutator;
    this.vlessLinkBuilder = vlessLinkBuilder;
    this.trafficMapper = trafficMapper;
  }

  @Override
  public String getVlessConfig(Server server, User user) {
    RestClient restClient = sessionClient.buildPanelClient(server);

    try {
      PreparedSession preparedSession = prepareSessionWithSyncedClient(server, user, restClient);
      XuiInboundsResponse inbounds = preparedSession.inbounds();
      return findVlessConfigInInbounds(
          inbounds, user, server.getIp(), server.getCountryEmoji());
    } catch (NotFoundException e) {
      throw new NotFoundException(String.format("Config for user %s not found on server with id %d",
          user.getFullName(), server.getId()));
    } catch (XuiUnavailableException e) {
      throw e;
    } catch (Exception e) {
      throw new XuiUnavailableException(
          "Unexpected error while preparing/getting config from XUI server: " + server.getName(), e);
    }
  }

  /**
   * Запрашивает JSON-конфиг. subPort вычисляется один раз на запрос и
   * переиспользуется через request-scope кэш.
   */
  @Override
  public String getJsonConfig(Server server, User user) {
    RestClient panelRestClient = sessionClient.buildPanelClient(server);
    RestClient jsonRestClient = sessionClient.buildRootClient(server);

    try {
      PreparedSession preparedSession = prepareSessionWithSyncedClient(server, user, panelRestClient);

      if (user.getXuiSubscription() == null) {
        throw new XuiUnavailableException("Missing XUI subscription id for user: " + user.getId());
      }

      Integer cachedSubPort = null;
      RequestScopedSubPortCache subPortCache;
      if (RequestContextHolder.getRequestAttributes() == null) {
        subPortCache = null;
      } else {
        try {
          subPortCache = subPortCacheProvider.getIfAvailable();
        } catch (ScopeNotActiveException e) {
          subPortCache = null;
        }
      }
      if (subPortCache != null) {
        cachedSubPort = subPortCache.get(server.getId());
      }

      int subscriptionPort = cachedSubPort != null
          ? cachedSubPort
          : jsonConfigClient.resolveSubscriptionPort(
              panelRestClient, preparedSession.sessionCookie(), server);

      if (subPortCache != null && cachedSubPort == null) {
        subPortCache.put(server.getId(), subscriptionPort);
      }

      String rootJsonPath = jsonConfigClient.buildJsonSubscriptionUrl(server, user, subscriptionPort);
      String jsonConfig = jsonConfigClient.fetchJsonConfigAtPath(
          jsonRestClient, preparedSession.sessionCookie(), rootJsonPath, server);
      return jsonConfigClient.replaceRemarksWithServerName(jsonConfig, server);
    } catch (XuiUnavailableException e) {
      throw e;
    } catch (RestClientException e) {
      log.warn("Failed to get JSON config from XUI server. serverId={}, userId={}, xuiId={}",
          server.getId(), user.getId(), user.getXuiId(), e);
      throw new XuiUnavailableException(
          "Failed to get JSON config from XUI server: " + server.getName(), e);
    } catch (Exception e) {
      throw new XuiUnavailableException(
          "Unexpected error while getting JSON config from XUI server: " + server.getName(), e);
    }
  }

  @Override
  public void createClient(Server server, User user) {
    RestClient restClient = sessionClient.buildPanelClient(server);
    upsertClient(server, user, restClient);
  }

  /**
   * Логинится, синхронизирует клиента (add/update) и возвращает inbounds
   * без повторного сетевого запроса.
   */
  private PreparedSession prepareSessionWithSyncedClient(
      Server server,
      User user,
      RestClient restClient) {
    String sessionCookie = sessionClient.login(restClient, server);
    XuiInboundsResponse inbounds = upsertClientAndReturnInbounds(server, user, restClient, sessionCookie);
    return new PreparedSession(sessionCookie, inbounds);
  }

  private XuiInboundsResponse upsertClientAndReturnInbounds(
      Server server,
      User user,
      RestClient restClient) {
    String sessionCookie = sessionClient.login(restClient, server);
    return upsertClientAndReturnInbounds(server, user, restClient, sessionCookie);
  }

  /**
   * Выполняет upsert клиента и локально обновляет настройки inbound,
   * чтобы не делать повторный GET inbounds.
   */
  private XuiInboundsResponse upsertClientAndReturnInbounds(
      Server server,
      User user,
      RestClient restClient,
      String sessionCookie) {
    XuiInboundsResponse inbounds = inboundClient.getInbounds(restClient, sessionCookie);

    Optional<XuiInboundsResponse.Inbound> vlessInboundOpt = findVlessInbound(inbounds);

    if (vlessInboundOpt.isEmpty()) {
      throw new XuiUnavailableException("No VLESS inbound found on server: " + server.getName());
    }

    XuiInboundsResponse.Inbound inbound = vlessInboundOpt.get();
    Optional<XuiClient> existingClient = inboundMutator.findClientInInbound(inbound, user);

    Subscription subscription = subscriptionService.findLastSubscriptionEntityByUserId(user.getId());
    Tariff tariff = subscription.getTariff();
    ObjectNode clientPayload = payloadBuilder.buildClientPayload(user, subscription, tariff,
        existingClient.orElse(null));
    String clientSettings;
    try {
      clientSettings = payloadBuilder.wrapClientPayload(clientPayload);
    } catch (JsonProcessingException e) {
      log.error(
          "XUI client payload serialization failed. serverId={}, userId={}, xuiId={}",
          server.getId(),
          user.getId(),
          user.getXuiId(),
          e);
      throw new XuiUnavailableException("Failed to prepare client settings");
    }

    if (existingClient.isPresent()) {
      inboundClient.updateClientInInbound(restClient, sessionCookie, inbound, user, server,
          existingClient.get(), clientSettings);
      inboundMutator.applyClientPayloadToInbound(inbound, clientPayload, existingClient.get());
      return inbounds;
    }

    inboundClient.addClientToInbound(restClient, sessionCookie, inbound, user, server, clientSettings);
    inboundMutator.applyClientPayloadToInbound(inbound, clientPayload, null);
    return inbounds;
  }

  private void upsertClient(Server server, User user, RestClient restClient) {
    upsertClientAndReturnInbounds(server, user, restClient);
  }

  private record PreparedSession(String sessionCookie, XuiInboundsResponse inbounds) {}

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
        .flatMap(inbound -> inboundMutator.findClientInInbound(inbound, user)
            .stream()
            .map(client -> Pair.of(inbound, client)))
        .map(pair ->
            vlessLinkBuilder.generateVlessLink(pair.getSecond(), serverIp, countryEmoji, pair.getFirst()))
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

  /**
   * Разбирает traffic-ответ XUI. Если obj пустой, возвращает "нулевой" трафик.
   */
  @Override
  public XuiClientTraffic getClientTraffic(Server server, String clientId) {
    RestClient restClient = sessionClient.buildPanelClient(server);

    try {
      String sessionCookie = sessionClient.login(restClient, server);

      String encodedClientId = UriUtils.encodePathSegment(clientId, StandardCharsets.UTF_8);
      String uri = "/panel/api/inbounds/getClientTrafficsById/" + encodedClientId;

      RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uri);
      if (sessionCookie != null) {
        request = request.header(HttpHeaders.COOKIE, sessionCookie);
      }

      String responseBody = request
          .retrieve()
          .body(String.class);

      return trafficMapper.mapTrafficResponse(responseBody, server, clientId);

    } catch (XuiUnavailableException e) {
      throw e;
    } catch (RestClientException e) {
      log.warn("Failed to get traffic from XUI server. serverId={}, clientId={}",
          server.getId(), clientId, e);
      throw new XuiUnavailableException(
          "Failed to get traffic from XUI server: " + server.getName(), e);
    } catch (Exception e) {
      log.error("Unexpected error getting traffic from server {}. clientId={}",
          server.getId(), clientId, e);
      throw new XuiUnavailableException(
          "Unexpected error while getting traffic: " + server.getName(), e);
    }
  }
}
