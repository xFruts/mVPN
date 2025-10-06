package ru.maxow.mvpn.promocode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.subscription.SubscriptionRequestDto;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.subscription.SubscriptionType;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.user.UserService;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PromocodeFacadeImpl implements PromocodeFacade {

  PromocodeService promocodeService;
  UserService userService;
  UserRepository userRepository;
  SubscriptionService subscriptionService;

  @Override
  @Transactional
  public void applyPromocode(Long userId, String code) {
    log.info("Applying promocode {} for user {}", code, userId);

    if (!userRepository.existsById(userId)) {
      throw new NotFoundException("User", userId);
    }
    if (userService.hasActiveSubscriptions(userId)) {
      throw new BadRequestException("User has active subscriptions");
    }

    // Validate and use the promocode
    // This will throw exceptions if the code is invalid or cannot be used
    promocodeService.usePromocode(code);

    SubscriptionRequestDto dto = new SubscriptionRequestDto(SubscriptionType.TRIAL);
    subscriptionService.createSubscription(userId, dto);
    log.info("Promocode {} applied successfully for user {}", code, userId);
  }
}
