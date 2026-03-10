package ru.maxow.mvpn.subscription;

import ru.maxow.mvpn.model.CreateUpdateSubscriptionDto;
import ru.maxow.mvpn.model.SubscriptionResponseDto;

import java.util.List;


public interface SubscriptionService {

  SubscriptionResponseDto createSubscription(Long userId, CreateUpdateSubscriptionDto subscriptionRequestDto);

  SubscriptionResponseDto updateSubscription(Long id, CreateUpdateSubscriptionDto subscriptionRequestDto);

  void deleteSubscription(Long id);

  SubscriptionResponseDto findSubscriptionById(Long id);

  List<SubscriptionResponseDto> findSubscriptionsByUserId(Long id);

  void extendSubscription(Long userId, String billingMonth);
}
