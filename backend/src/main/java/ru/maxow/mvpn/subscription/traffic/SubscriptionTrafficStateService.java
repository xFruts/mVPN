package ru.maxow.mvpn.subscription.traffic;

import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.util.Optional;

public interface SubscriptionTrafficStateService {

  SubscriptionTrafficState syncTrafficForSubscription(
      User user,
      Subscription subscription
  ) throws XuiUnavailableException;

  Optional<SubscriptionTrafficState> getTrafficStateBySubscriptionId(
      Long subscriptionId
  );
}
