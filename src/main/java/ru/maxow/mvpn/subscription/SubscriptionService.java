package ru.maxow.mvpn.subscription;

public interface SubscriptionService {
  SubscriptionResponseDto createSubscription(Long userId, SubscriptionRequestDto subscriptionRequestDto);

  SubscriptionResponseDto updateSubscription(Long id, SubscriptionRequestDto subscriptionRequestDto);

  void deleteSubscription(Long id);

  Subscription findSubscriptionById(Long id);
}
