package ru.maxow.mvpn.xui.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.SubscriptionFormat;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.xui.dto.SubscriptionConfigPayload;
import ru.maxow.mvpn.xui.XuiPanelService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Background synchronization: ensure XUI has clients for user's subscription.
 * Runs asynchronously after we returned cached config to user.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigSyncService {
  private final UserRepository userRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final XuiPanelService xuiPanelService;
  private final ConfigCacheService configCacheService;

  @Async
  @Transactional(readOnly = true)
  public void asyncSyncSubscription(UUID verificationCode) {
    try {
      User user = userRepository.findByVerificationCode(verificationCode).orElse(null);
      if (user == null) {
        log.warn("asyncSync: user not found for verificationCode={}", verificationCode);
        return;
      }

      Subscription subscription = subscriptionRepository
          .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(user.getId())
          .orElse(null);
      if (subscription == null) {
        log.warn("asyncSync: subscription not found for user={}", user.getId());
        return;
      }

      Set<Server> servers = subscription.getTariff().getServers();
      for (Server server : servers) {
        try {
          xuiPanelService.createOrUpdateClient(server, user, subscription);
        } catch (Exception e) {
          log.warn("asyncSync: failed to create/sync client on server {} for user {}: {}",
              server.getId(), user.getId(), e.getMessage());
        }
      }

      try {
        record LocalResolved(Server server, String config, SubscriptionFormat format) {
        }

        List<LocalResolved> configs = new ArrayList<>();
        for (Server server : servers) {
          try {
            SubscriptionFormat format = server.getSubscriptionFormat() == null
                ? SubscriptionFormat.VLESS
                : server.getSubscriptionFormat();
            String config = format == SubscriptionFormat.JSON
                ? xuiPanelService.getJsonConfig(server, user)
                : xuiPanelService.getVlessConfig(server, user);
            configs.add(new LocalResolved(server, config, format));
          } catch (Exception e) {
            log.warn("asyncSync: failed to fetch config from server {}: {}",
                server.getId(), e.getMessage());
          }
        }

        if (!configs.isEmpty()) {
          boolean isJson = configs.stream()
              .allMatch(c -> c.format == SubscriptionFormat.JSON);
          if (isJson) {
            String combined = configs.stream()
                .map(c -> c.config)
                .collect(Collectors.joining(",", "[", "]"));
            configCacheService.put(user.getId(),
                new SubscriptionConfigPayload(combined, SubscriptionFormat.JSON));
          } else {
            String combined = configs.stream().map(c -> c.config)
                .collect(Collectors.joining("\n"));
            String encoded = java.util.Base64.getEncoder()
                .encodeToString(combined.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            configCacheService.put(user.getId(),
                new SubscriptionConfigPayload(encoded, SubscriptionFormat.VLESS));
          }
        }
      } catch (Exception e) {
        log.warn("asyncSync: failed to refresh cache for verificationCode={} : {}",
            verificationCode, e.getMessage());
      }

    } catch (Exception e) {
      log.error("asyncSync: unexpected error for verificationCode={}", verificationCode, e);
    }
  }
}
