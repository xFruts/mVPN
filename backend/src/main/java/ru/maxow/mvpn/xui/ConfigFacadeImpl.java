package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.ServerRepository;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.tariff.TariffRepository;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFacadeImpl implements ConfigFacade {

  XuiPanelService xuiPanelService;
  UserRepository userRepository;
  SubscriptionRepository subscriptionRepository;

  @Override
  public String getSubscriptionConfig(UUID verificationCode) {
    User user = userRepository.findByVerificationCode(verificationCode)
        .orElseThrow(() -> new NotFoundException("User by verification code"));

    Subscription subscription = subscriptionRepository
        .findFirstByUser_IdOrderByStartDateDesc(user.getId())
        .orElseThrow(() -> new NotFoundException("Subscription for user"));

    if (subscription.getStatus() != SubscriptionStatus.ACTIVE ||
        subscription.getEndDate().isBefore(java.time.OffsetDateTime.now())) {
      throw new BadRequestException("Subscription is not active or expired for user");
    }

    List<String> configs = getActiveServersByUserSubscription(subscription).stream()
        .map(server -> {
          try {
            return xuiPanelService.getVlessConfig(server, user);
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

    String combinedConfigs = String.join("\n", configs);
    return Base64.getEncoder().encodeToString(combinedConfigs.getBytes(StandardCharsets.UTF_8));
  }

  private List<Server> getActiveServersByUserSubscription(Subscription subscription) {
    return subscription.getTariff().getServers().stream()
        .filter(server -> server.getStatus() == ServerStatus.ACTIVE)
        .toList();
  }
}
