package ru.maxow.mvpn.xui.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.SubscriptionFormat;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.xui.XuiPanelService;
import ru.maxow.mvpn.xui.dto.SubscriptionConfigPayload;

/**
 * Сервис асинхронной фоновой синхронизации подписок и обновления кэша конфигураций.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigSyncService {

  UserRepository userRepository;
  SubscriptionRepository subscriptionRepository;
  XuiPanelService xuiPanelService;
  ConfigCacheService configCacheService;
  ObjectMapper objectMapper;

  @Async
  public void asyncSyncSubscription(UUID verificationCode) {
    try {
      User user = userRepository.findByVerificationCode(verificationCode)
          .orElseThrow(() -> new NotFoundException(
              "User not found by verification code: " + verificationCode));

      Subscription subscription = subscriptionRepository
          .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(user.getId())
          .orElseThrow(() -> new NotFoundException(
              "Active subscription not found for user: " + user.getId()));

      List<ResolvedServerConfig> successfulConfigs = syncActiveServers(user, subscription);

      if (successfulConfigs.isEmpty()) {
        log.warn("asyncSync: no active servers responded for user id={}", user.getId());
        return;
      }

      updateCache(user.getId(), successfulConfigs);

    } catch (NotFoundException e) {
      log.warn("asyncSync: synchronization skipped: {}", e.getMessage());
    } catch (Exception e) {
      log.error("asyncSync: unexpected error for verificationCode={}: {}",
          verificationCode, e.getMessage(), e);
    }
  }

  private List<ResolvedServerConfig> syncActiveServers(User user, Subscription subscription) {
    List<Server> activeServers = subscription.getTariff().getServers().stream()
        .filter(server -> server.getStatus() == ServerStatus.ACTIVE)
        .toList();

    List<ResolvedServerConfig> resolved = new ArrayList<>();
    for (Server server : activeServers) {
      ResolvedServerConfig config = syncSingleServer(server, user, subscription);
      if (config != null) {
        resolved.add(config);
      }
    }
    return resolved;
  }

  private ResolvedServerConfig syncSingleServer(
      Server server,
      User user,
      Subscription subscription) {
    try {
      xuiPanelService.createOrUpdateClient(server, user, subscription);

      SubscriptionFormat format = server.getSubscriptionFormat();
      String config = format == SubscriptionFormat.JSON
          ? xuiPanelService.getJsonConfig(server, user)
          : xuiPanelService.getVlessConfig(server, user);

      return new ResolvedServerConfig(server, config, format);
    } catch (Exception e) {
      log.warn("asyncSync: failed to sync server {} ('{}') for user id={}: {}",
          server.getId(), server.getName(), user.getId(), e.getMessage());
      return null;
    }
  }

  private void updateCache(Long userId, List<ResolvedServerConfig> configs) {
    try {
      boolean isAllJson = configs.stream()
          .allMatch(c -> c.format() == SubscriptionFormat.JSON);
      SubscriptionConfigPayload payload = isAllJson
          ? buildCombinedJsonPayload(configs)
          : buildCombinedVlessPayload(configs);

      if (payload != null) {
        configCacheService.put(userId, payload);
      }
    } catch (Exception e) {
      log.warn("asyncSync: failed to refresh config cache for user id={}: {}",
          userId, e.getMessage());
    }
  }

  private SubscriptionConfigPayload buildCombinedJsonPayload(List<ResolvedServerConfig> configs) {
    ArrayNode jsonArray = objectMapper.createArrayNode();
    for (ResolvedServerConfig cfg : configs) {
      try {
        jsonArray.add(objectMapper.readTree(cfg.config()));
      } catch (Exception e) {
        log.warn("asyncSync: failed to parse JSON for server {}, skipping", cfg.server().getId());
      }
    }

    if (jsonArray.isEmpty()) {
      return null;
    }

    try {
      String combined = objectMapper.writeValueAsString(jsonArray);
      return new SubscriptionConfigPayload(combined, SubscriptionFormat.JSON, null);
    } catch (Exception e) {
      log.error("asyncSync: failed to serialize combined JSON configs", e);
      return null;
    }
  }

  private SubscriptionConfigPayload buildCombinedVlessPayload(List<ResolvedServerConfig> configs) {
    String combined = configs.stream()
        .map(ResolvedServerConfig::config)
        .collect(java.util.stream.Collectors.joining("\n"));
    String encoded = Base64.getEncoder().encodeToString(
        combined.getBytes(StandardCharsets.UTF_8));
    return new SubscriptionConfigPayload(encoded, SubscriptionFormat.VLESS, null);
  }
}
