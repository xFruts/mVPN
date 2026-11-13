package ru.maxow.mvpn.telegram.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.ServerRepository;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficStateService;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;
import ru.maxow.mvpn.xui.XuiPanelService;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TelegramUserService {

  UserService userService;
  SubscriptionRepository subscriptionRepository;
  SubscriptionTrafficStateService trafficStateService;
  ServerRepository serverRepository;
  XuiPanelService xuiPanelService;
  String profileWebPageUrl;

  public TelegramUserService(
      UserService userService,
      SubscriptionRepository subscriptionRepository,
      SubscriptionTrafficStateService trafficStateService,
      ServerRepository serverRepository,
      XuiPanelService xuiPanelService,
      @Value("${vpn.config.profile-web-page-url:}") String profileWebPageUrl
  ) {
    this.userService = userService;
    this.subscriptionRepository = subscriptionRepository;
    this.trafficStateService = trafficStateService;
    this.serverRepository = serverRepository;
    this.xuiPanelService = xuiPanelService;
    this.profileWebPageUrl = profileWebPageUrl;
  }

  @Transactional
  public Optional<User> findAuthenticatedUser(Long telegramId, String telegramUsername) {
    if (telegramId == null) {
      return Optional.empty();
    }
    User user = userService.findByTelegramId(telegramId);
    if (user != null) {
      return Optional.of(user);
    }
    if (telegramUsername != null && !telegramUsername.isBlank()) {
      Optional<User> matched = userService.findByTelegramUsername(telegramUsername);
      if (matched.isPresent()) {
        User u = matched.get();
        log.info("Auto-authenticating user id={} by Telegram username '@{}' (chatId={})",
            u.getId(), telegramUsername, telegramId);
        userService.bindTelegramIdToUser(u.getId(), telegramId);
        u.setUserTelegramId(telegramId);
        return Optional.of(u);
      }
    }
    return Optional.empty();
  }

  @Transactional
  public Optional<User> findAuthenticatedUser(Long telegramId) {
    return findAuthenticatedUser(telegramId, null);
  }

  @Transactional
  public boolean authenticateUser(UUID token, Long telegramId) {
    if (token == null || telegramId == null) {
      return false;
    }
    if (!userService.checkVerificationCode(token)) {
      return false;
    }
    userService.updateUserTelegramId(token, telegramId);
    return true;
  }

  @Transactional(readOnly = true)
  public Optional<Subscription> findLatestSubscription(User user) {
    if (user == null || user.getId() == null) {
      return Optional.empty();
    }
    return subscriptionRepository
        .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(user.getId());
  }

  @Transactional(readOnly = true)
  public Optional<SubscriptionTrafficState> getTrafficState(Long subscriptionId) {
    if (subscriptionId == null) {
      return Optional.empty();
    }
    return trafficStateService.getTrafficStateBySubscriptionId(subscriptionId);
  }

  @Transactional(readOnly = true)
  public List<Server> getAvailableServers(User user) {
    Set<Server> servers = null;
    if (user != null && user.getId() != null) {
      Optional<Subscription> latest = subscriptionRepository
          .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(user.getId());
      if (latest.isPresent() && latest.get().getTariff() != null) {
        servers = latest.get().getTariff().getServers();
      }
    }

    if (servers == null || servers.isEmpty()) {
      servers = Set.copyOf(serverRepository.findAll());
    }

    return servers.stream()
        .sorted(Comparator.comparing(this::statusOrder)
            .thenComparing(s -> s.getName() != null ? s.getName() : ""))
        .toList();
  }

  public String buildSubscriptionUrl(User user) {
    if (user == null || user.getVerificationCode() == null) {
      return "";
    }
    if (profileWebPageUrl != null && !profileWebPageUrl.isBlank()) {
      String trimmed = profileWebPageUrl.replaceAll("/+$", "");
      return trimmed + "/" + user.getVerificationCode();
    }
    return "/v1/config/" + user.getVerificationCode();
  }

  public Optional<String> getDirectVlessKey(User user) {
    if (user == null) {
      return Optional.empty();
    }
    List<Server> servers = getAvailableServers(user);
    for (Server server : servers) {
      if (server.getStatus() == ServerStatus.ACTIVE) {
        try {
          String vless = xuiPanelService.getVlessConfig(server, user);
          if (vless != null && !vless.isBlank()) {
            return Optional.of(vless);
          }
        } catch (Exception e) {
          log.warn("Failed to get direct VLESS config from server {}: {}",
              server.getName(), e.getMessage());
        }
      }
    }
    return Optional.empty();
  }

  private int statusOrder(Server server) {
    if (server.getStatus() == ServerStatus.ACTIVE) {
      return 1;
    }
    if (server.getStatus() == ServerStatus.MAINTENANCE) {
      return 2;
    }
    return 3;
  }
}
