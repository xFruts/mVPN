package ru.maxow.mvpn.subscription;

import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;

import java.util.List;
import java.util.UUID;


public interface SubscriptionService {

  SubscriptionResponseDto createSubscription(Long userId,
                                             CreateUpdateSubscriptionDto subscriptionRequestDto);

  SubscriptionResponseDto updateSubscription(Long id,
                                             CreateUpdateSubscriptionDto subscriptionRequestDto);

  void deleteSubscription(Long id);

  SubscriptionResponseDto findSubscriptionById(Long id);

  List<SubscriptionResponseDto> findSubscriptionsByUserId(Long id);

  String getSubscriptionInfoForUserByCode(UUID verificationCode);

  void extendSubscription(Long userId, String billingDate);

  Subscription findLastSubscriptionEntityByUserId(Long userId);

  SubscriptionResponseDto getLastSubscriptionByUserId(Long userId);

  void extendSubscriptionsByUserIds(List<Long> userIds);
}
