package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.SubscriptionFormat;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFacadeImpl implements ConfigFacade {

  XuiPanelService xuiPanelService;
  UserRepository userRepository;
  SubscriptionRepository subscriptionRepository;
  ObjectMapper objectMapper;

  @Override
  @Transactional(readOnly = true)
  public SubscriptionConfigPayload getSubscriptionConfig(UUID verificationCode) {
    User user = userRepository.findByVerificationCode(verificationCode)
        .orElseThrow(() -> new NotFoundException("User by verification code"));

    Subscription subscription = subscriptionRepository
        .findFirstByUser_IdOrderByStartDateDesc(user.getId())
        .orElseThrow(() -> new NotFoundException("Subscription for user"));

    if (subscription.getStatus() != SubscriptionStatus.ACTIVE ||
        subscription.getEndDate().isBefore(java.time.OffsetDateTime.now())) {
      throw new BadRequestException("Subscription is not active or expired for user");
    }

    List<ResolvedServerConfig> configs = getActiveServersByUserSubscription(subscription).stream()
        .map(server -> {
          try {
            SubscriptionFormat format = resolveSubscriptionFormat(server);
            String config = format == SubscriptionFormat.JSON
                ? xuiPanelService.getJsonConfig(server, user)
                : xuiPanelService.getVlessConfig(server, user);
            return new ResolvedServerConfig(server, config, format);
          } catch (XuiUnavailableException e) {
            log.warn("XUI is unavailable for server {}: {}", server, e.getMessage());
            return null;
          } catch (RuntimeException e) {
            log.error("Unexpected error while getting config from server {}: {}",
                server, e.getMessage(), e);
            return null;
          }
        })
        .filter(java.util.Objects::nonNull)
        .toList();

    if (configs.isEmpty()) {
      throw new NotFoundException("No configs found for user");
    }

    return resolvePayload(configs);
  }

  private List<Server> getActiveServersByUserSubscription(Subscription subscription) {
    return subscription.getTariff().getServers().stream()
        .filter(server -> server.getStatus() == ServerStatus.ACTIVE)
        .toList();
  }

    private SubscriptionConfigPayload resolvePayload(List<ResolvedServerConfig> configs) {
      Set<SubscriptionFormat> formats = configs.stream()
          .map(ResolvedServerConfig::format)
          .collect(java.util.stream.Collectors.toSet());

      if (formats.size() > 1) {
        throw new BadRequestException(
            "Mixed subscription formats are not supported for a single subscription");
      }

      SubscriptionFormat format = formats.iterator().next();

      if (format == SubscriptionFormat.JSON) {
        try {
          ArrayNode jsonArray = objectMapper.createArrayNode();
          for (ResolvedServerConfig cfg : configs) {
            try {
              jsonArray.add(objectMapper.readTree(cfg.config()));
            } catch (Exception e) {
              log.warn("Failed to parse JSON config for server {}, skipping. Error: {}",
                  cfg.server().getId(), e.getMessage());
            }
          }

          if (jsonArray.isEmpty()) {
            throw new BadRequestException("No valid JSON configs found to combine");
          }

          String combinedConfigs = objectMapper.writeValueAsString(jsonArray);
          log.debug("Combined {} JSON configs into array", jsonArray.size());
          return new SubscriptionConfigPayload(combinedConfigs, SubscriptionFormat.JSON);
        } catch (BadRequestException e) {
          throw e;
        } catch (Exception e) {
          log.error("Failed to combine JSON configs", e);
          throw new BadRequestException("Failed to combine JSON configs: " + e.getMessage());
        }
      }

      String combinedConfigs = String.join("\n", configs.stream()
          .map(ResolvedServerConfig::config)
          .toList());
      String encoded = Base64.getEncoder().encodeToString(
          combinedConfigs.getBytes(StandardCharsets.UTF_8));
      return new SubscriptionConfigPayload(encoded, SubscriptionFormat.VLESS);
    }

  private SubscriptionFormat resolveSubscriptionFormat(Server server) {
    return server.getSubscriptionFormat() == null
        ? SubscriptionFormat.VLESS
        : server.getSubscriptionFormat();
  }

  private record ResolvedServerConfig(Server server, String config, SubscriptionFormat format) {}
}
