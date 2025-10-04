package ru.maxow.mvpn.subscription;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.vpnconfig.VpnConfigRepository;
import ru.maxow.mvpn.vpnconfig.XrayConfig;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionServiceImpl implements SubscriptionService {

  SubscriptionRepository subscriptionRepository;
  SubscriptionMapper subscriptionMapper;
  UserService userService;
  VpnConfigRepository vpnConfigRepository;

  @Override
  public SubscriptionResponseDto createSubscription(
      Long userId, SubscriptionRequestDto subscriptionRequestDto) {
    User user = userService.findById(userId);

    SubscriptionType type = SubscriptionType.valueOf(subscriptionRequestDto.configType());

    if (type == SubscriptionType.TRIAL) {
      return createTrialSubscription(userId);
    }
  }

  private SubscriptionResponseDto createTrialSubscription(Long userId) {
    User user = userService.findById(userId);

    if (subscriptionRepository.existsByUserAndType(user, SubscriptionType.TRIAL)) {
      throw new BadRequestException("Subscription already exists");
    }

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setType(SubscriptionType.TRIAL);
    subscription.setStartDate(java.time.LocalDateTime.now());
    subscription.setEndDate(subscription.getStartDate().plusDays(7));
    subscription.setStatus(SubscriptionStatus.ACTIVE);

    XrayConfig xrayConfig = new XrayConfig();
    vpnConfigRepository.save(xrayConfig);
    subscription.setVpnConfig(xrayConfig);

    Subscription savedSubscription = subscriptionRepository.save(subscription);
    log.info("Trial subscription created for user with id: {}", userId);
    return subscriptionMapper.toSubscriptionResponseDto(savedSubscription);
  }

  private SubscriptionResponseDto createPaidSubscription(Long userId, SubscriptionRequestDto subscriptionRequestDto) {
    User user = userService.findById(userId);

    SubscriptionType type = SubscriptionType.valueOf(subscriptionRequestDto.configType());

    if (type == SubscriptionType.TRIAL) {
      throw new BadRequestException("Invalid subscription type");
    }

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setType(type);
    subscription.setStartDate(java.time.LocalDateTime.now());

    switch (type) {
      case MONTHLY -> subscription.setEndDate(subscription.getStartDate().plusMonths(1));
      case YEARLY -> subscription.setEndDate(subscription.getStartDate().plusYears(1));
      default -> throw new BadRequestException("Invalid subscription type");
    }

    subscription.setStatus(SubscriptionStatus.ACTIVE);

    XrayConfig xrayConfig = new XrayConfig();
    vpnConfigRepository.save(xrayConfig);
    subscription.setVpnConfig(xrayConfig);

    Subscription savedSubscription = subscriptionRepository.save(subscription);
    log.info("Paid subscription created for user with id: {}", userId);
    return subscriptionMapper.toSubscriptionResponseDto(savedSubscription);
  }

  @Override
  public SubscriptionResponseDto updateSubscription(
      Long id, SubscriptionRequestDto subscriptionRequestDto) {
    return null;
  }

  @Override
  public void deleteSubscription(Long id) {

  }

  @Override
  public Subscription findSubscriptionById(Long id) {
    return null;
  }
}
