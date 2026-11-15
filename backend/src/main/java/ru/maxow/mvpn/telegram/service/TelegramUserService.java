package ru.maxow.mvpn.telegram.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.device.GeoIpInfo;
import ru.maxow.mvpn.device.GeoIpService;
import ru.maxow.mvpn.device.UserDevice;
import ru.maxow.mvpn.device.UserDeviceService;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.ServerRepository;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficStateService;
import ru.maxow.mvpn.telegram.model.UserDeviceRecord;
import ru.maxow.mvpn.telegram.model.UserDevicesSummary;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;
import ru.maxow.mvpn.xui.XuiPanelService;
import ru.maxow.mvpn.xui.dto.XuiClientIpRecord;

/**
 * Сервис интеграции бизнес-логики пользователей, подписок и сессий для Telegram-бота.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TelegramUserService {

  private static final String DEFAULT_API_DOMAIN = "api.vpn.maxow.ru";

  final UserService userService;
  final SubscriptionRepository subscriptionRepository;
  final SubscriptionTrafficStateService trafficStateService;
  final ServerRepository serverRepository;
  final XuiPanelService xuiPanelService;

  @Autowired(required = false)
  @Setter
  GeoIpService geoIpService;

  @Autowired(required = false)
  @Setter
  UserDeviceService userDeviceService;

  @Value("${vpn.config.profile-web-page-url:}")
  @Setter
  String profileWebPageUrl;

  @Value("${vpn.config.subscription-base-url:}")
  @Setter
  String subscriptionBaseUrl;

  @Value("${vpn.config.api-domain:api.vpn.maxow.ru}")
  @Setter
  String apiDomain = DEFAULT_API_DOMAIN;

  /**
   * Находит авторизованного пользователя по Telegram ID или связывает по username.
   *
   * @param telegramId идентификатор Telegram
   * @param telegramUsername имя пользователя в Telegram
   * @return {@link Optional} с найденным пользователем или пустой
   */
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
        log.info("Auto-authenticating user id={} via Telegram username match", u.getId());
        userService.bindTelegramIdToUser(u.getId(), telegramId);
        u.setUserTelegramId(telegramId);
        return Optional.of(u);
      }
    }
    return Optional.empty();
  }

  /**
   * Находит авторизованного пользователя только по Telegram ID.
   *
   * @param telegramId идентификатор Telegram
   * @return {@link Optional} с найденным пользователем или пустой
   */
  @Transactional
  public Optional<User> findAuthenticatedUser(Long telegramId) {
    return findAuthenticatedUser(telegramId, null);
  }

  /**
   * Авторизует пользователя в боте по коду верификации.
   *
   * @param token токен/код верификации пользователя
   * @param telegramId идентификатор Telegram для привязки
   * @return {@code true}, если авторизация прошла успешно
   */
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

  /**
   * Возвращает последнюю подписку пользователя.
   *
   * @param user сущность пользователя
   * @return {@link Optional} с последней подпиской или пустой
   */
  @Transactional(readOnly = true)
  public Optional<Subscription> findLatestSubscription(User user) {
    if (user == null || user.getId() == null) {
      return Optional.empty();
    }
    return subscriptionRepository
        .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(user.getId());
  }

  /**
   * Получает актуальное состояние трафика подписки с синхронизацией.
   *
   * @param user сущность пользователя
   * @param subscriptionId идентификатор подписки
   * @return {@link Optional} с состоянием трафика или пустой
   */
  @Transactional
  public Optional<SubscriptionTrafficState> getTrafficState(User user, Long subscriptionId) {
    if (subscriptionId == null) {
      return Optional.empty();
    }
    if (user != null) {
      Optional<Subscription> subOpt = subscriptionRepository.findById(subscriptionId);
      if (subOpt.isPresent()) {
        try {
          SubscriptionTrafficState synced = trafficStateService
              .syncTrafficForSubscription(user, subOpt.get());
          if (synced != null) {
            return Optional.of(synced);
          }
        } catch (Exception e) {
          log.warn("Live traffic sync failed for user id={}: {}", user.getId(), e.getMessage());
        }
      }
    }
    return trafficStateService.getTrafficStateBySubscriptionId(subscriptionId);
  }

  /**
   * Возвращает список серверов, доступных пользователю по его подписке.
   *
   * @param user сущность пользователя
   * @return отсортированный список серверов
   */
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

  /**
   * Формирует персональный URL подписки для подключения клиента.
   *
   * @param user сущность пользователя
   * @return URL ссылки на подписку
   */
  public String buildSubscriptionUrl(User user) {
    if (user == null || user.getVerificationCode() == null) {
      return "";
    }
    if (subscriptionBaseUrl != null && !subscriptionBaseUrl.isBlank()) {
      String trimmed = subscriptionBaseUrl.replaceAll("/+$", "");
      return trimmed + "/" + user.getVerificationCode();
    }
    if (apiDomain != null && !apiDomain.isBlank()) {
      String trimmed = apiDomain.replaceAll("^https?://", "").replaceAll("/+$", "");
      return "https://" + trimmed + "/v1/config/" + user.getVerificationCode();
    }
    if (profileWebPageUrl != null && !profileWebPageUrl.isBlank()
        && !profileWebPageUrl.contains("t.me/")) {
      String trimmed = profileWebPageUrl.replaceAll("/+$", "");
      return trimmed + "/" + user.getVerificationCode();
    }
    return "https://" + DEFAULT_API_DOMAIN + "/v1/config/" + user.getVerificationCode();
  }

  /**
   * Получает прямой VLESS ключ для первого доступного активного сервера.
   *
   * @param user сущность пользователя
   * @return {@link Optional} с VLESS конфигурацией или пустой
   */
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

  /**
   * Возвращает сводную информацию об активных и зарегистрированных устройствах.
   *
   * @param user сущность пользователя
   * @return сводка по устройствам {@link UserDevicesSummary}
   */
  @Transactional(readOnly = true)
  public UserDevicesSummary getConnectedDevices(User user) {
    if (user == null) {
      return new UserDevicesSummary(0, 1, 1, List.of(), List.of());
    }

    Optional<Subscription> subOpt = findLatestSubscription(user);
    int maxDevices = 1;
    if (subOpt.isPresent() && subOpt.get().getTariff() != null
        && subOpt.get().getTariff().getMaxDevices() != null) {
      maxDevices = subOpt.get().getTariff().getMaxDevices();
    }

    List<Server> servers = getAvailableServers(user);
    Map<String, DeviceAccumulator> ipMap = new LinkedHashMap<>();

    for (Server server : servers) {
      if (server.getStatus() != ServerStatus.ACTIVE) {
        continue;
      }
      try {
        List<XuiClientIpRecord> records =
            xuiPanelService.getClientIps(server, user.getFullName());
        String flag = server.getCountryEmoji() != null ? server.getCountryEmoji() + " " : "";
        String serverName = server.getName() != null ? server.getName() : "";
        String serverLabel = (flag + serverName).strip();
        if (serverLabel.isBlank()) {
          serverLabel = "Node-" + server.getId();
        }

        for (XuiClientIpRecord record : records) {
          String ip = record.ip();
          if (ip == null || ip.isBlank()) {
            continue;
          }
          DeviceAccumulator acc = ipMap.computeIfAbsent(ip, k -> new DeviceAccumulator());
          if (!acc.servers.contains(serverLabel)) {
            acc.servers.add(serverLabel);
          }
          if (record.timestamp() != null) {
            if (acc.timestamp == null || record.timestamp() > acc.timestamp) {
              acc.timestamp = record.timestamp();
            }
          }
        }
      } catch (Exception e) {
        log.warn("Failed to get client IPs for user id={} on server {}: {}",
            user.getId(), server.getName(), e.getMessage());
      }
    }

    List<UserDeviceRecord> devices = ipMap.entrySet().stream()
        .map(e -> {
          String ip = e.getKey();
          String ispLocation = geoIpService != null
              ? geoIpService.lookup(ip).map(GeoIpInfo::formatLocation).orElse(null)
              : null;
          return new UserDeviceRecord(
              ip,
              e.getValue().servers,
              ispLocation,
              e.getValue().timestamp
          );
        })
        .toList();

    List<UserDevice> registeredDevices = userDeviceService != null
        ? userDeviceService.getUserDevices(user)
        : List.of();

    int activeCount = devices.size();
    int remaining = Math.max(0, maxDevices - activeCount);

    return new UserDevicesSummary(activeCount, maxDevices, remaining, devices, registeredDevices);
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

  private static class DeviceAccumulator {
    final List<String> servers = new ArrayList<>();
    Long timestamp;
  }
}
