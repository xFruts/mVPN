package ru.maxow.mvpn.xui;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.dto.XuiInboundDto;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class XuiPanelServiceImpl implements XuiPanelService {

  XuiSessionClient sessionClient;
  XuiClientProvisioner clientProvisioner;
  XuiClientTrafficReader trafficReader;
  XuiVlessConfigService vlessConfigService;
  XuiInboundClient inboundClient;

  @Override
  public void testConnection(Server server) {
    executeWithHandling(server, "test connection", () -> {
      RestClient restClient = sessionClient.buildPanelClient(server);
      sessionClient.login(restClient, server);
      return null;
    });
  }

  @Override
  public List<XuiInboundDto> getInbounds(Server server) {
    return executeWithHandling(server, "get inbounds", () -> {
      RestClient restClient = sessionClient.buildPanelClient(server);
      String sessionCookie = sessionClient.login(restClient, server);
      return inboundClient.getInbounds(restClient, sessionCookie, server);
    });
  }

  @Override
  public String getVlessConfig(Server server, User user) {
    return executeWithHandling(server, "get VLESS config", () -> {
      RestClient restClient = sessionClient.buildPanelClient(server);
      String sessionCookie = sessionClient.login(restClient, server);
      return vlessConfigService.getVlessConfig(restClient, sessionCookie, server, user);
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

  private <T> T executeWithHandling(
      Server server,
      String operationDescription,
      PanelOperation<T> operation) {
    try {
      return operation.execute();
    } catch (XuiUnavailableException e) {
      throw e;
    } catch (Exception e) {
      log.warn("XUI server '{}' (id={}) is unreachable during '{}': {}",
          server.getName(), server.getId(), operationDescription, e.getMessage());
      throw new XuiUnavailableException(
          String.format("XUI is unavailable for server '%s' during '%s'",
              server.getName(), operationDescription), e);
    }
  }

  @FunctionalInterface
  private interface PanelOperation<T> {
    T execute() throws Exception;
  }
}
