package ru.maxow.mvpn.xui;

import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

/**
 * Фасад для операций с панелью 3X-UI: аутентификация, вызов специализированных сервисов
 * и централизованная обработка сетевых ошибок.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class XuiPanelServiceImpl implements XuiPanelService {

  XuiSessionClient sessionClient;
  XuiClientProvisioner clientProvisioner;
  XuiClientTrafficReader trafficReader;
  XuiVlessConfigService vlessConfigService;
  XuiJsonConfigClient jsonConfigClient;

  @Override
  public String getVlessConfig(Server server, User user) {
    return executeWithHandling(server, "get VLESS config", () -> {
      RestClient restClient = sessionClient.buildPanelClient(server);
      String sessionCookie = sessionClient.login(restClient, server);
      return vlessConfigService.resolveVlessConfig(restClient, sessionCookie, server, user);
    });
  }

  @Override
  public String getJsonConfig(Server server, User user) {
    return executeWithHandling(server, "get JSON config", () -> {
      if (user.getXuiSubscription() == null) {
        throw new XuiUnavailableException(
            "Missing XUI subscription id for user: " + user.getId());
      }

      RestClient panelRestClient = sessionClient.buildPanelClient(server);
      RestClient jsonRestClient = sessionClient.buildRootClient(server);

      String sessionCookie = sessionClient.login(panelRestClient, server);
      int subscriptionPort = jsonConfigClient.resolveSubscriptionPort(
          panelRestClient, sessionCookie, server);
      String rootJsonPath = jsonConfigClient.buildJsonSubscriptionUrl(
          server, user, subscriptionPort);
      String jsonConfig = jsonConfigClient.fetchJsonConfigAtPath(
          jsonRestClient, sessionCookie, rootJsonPath, server);

      return jsonConfigClient.replaceRemarksWithServerName(jsonConfig, server);
    });
  }

  @Override
  public void createOrUpdateClient(Server server, User user, Subscription subscription) {
    executeWithHandling(server, "create or update client", () -> {
      RestClient restClient = sessionClient.buildPanelClient(server);
      String sessionCookie = sessionClient.login(restClient, server);
      clientProvisioner.upsertClient(restClient, sessionCookie, server, user, subscription);
      return null;
    });
  }

  @Override
  public void createOrUpdateClient(
      Server server,
      User user,
      Subscription subscription,
      boolean enable) {
    executeWithHandling(server, "create or update client", () -> {
      RestClient restClient = sessionClient.buildPanelClient(server);
      String sessionCookie = sessionClient.login(restClient, server);
      clientProvisioner.upsertClient(
          restClient, sessionCookie, server, user, subscription, enable);
      return null;
    });
  }

  @Override
  public XuiClientTraffic getClientTraffic(Server server, String clientEmail) {
    return executeWithHandling(server, "get client traffic", () -> {
      RestClient restClient = sessionClient.buildPanelClient(server);
      String sessionCookie = sessionClient.login(restClient, server);
      return trafficReader.getClientTraffic(restClient, sessionCookie, server, clientEmail);
    });
  }

  private <T> T executeWithHandling(Server server, String operationName, Supplier<T> action) {
    try {
      return action.get();
    } catch (NotFoundException | XuiUnavailableException e) {
      throw e;
    } catch (ResourceAccessException e) {
      log.warn("XUI server '{}' (id={}) is unreachable during '{}': {}",
          server.getName(), server.getId(), operationName, e.getMessage());
      throw new XuiUnavailableException(String.format(
          "Network error while connecting to XUI server %s (%s)",
          server.getName(), operationName), e);
    } catch (RestClientException e) {
      log.warn("XUI server '{}' (id={}) REST API error during '{}': {}",
          server.getName(), server.getId(), operationName, e.getMessage());
      throw new XuiUnavailableException(String.format(
          "API error from XUI server %s (%s)", server.getName(), operationName), e);
    } catch (Exception e) {
      log.error("Unexpected error on XUI server '{}' (id={}) during '{}': {}",
          server.getName(), server.getId(), operationName, e.getMessage(), e);
      throw new XuiUnavailableException(String.format(
          "Unexpected error on XUI server %s (%s)", server.getName(), operationName), e);
    }
  }
}
