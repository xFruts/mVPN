package ru.maxow.mvpn.subscription.traffic;

import java.util.Optional;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

public interface SubscriptionTrafficStateService {

  SubscriptionTrafficState syncTrafficForSubscription(
      User user,
      Subscription subscription
  ) throws XuiUnavailableException;

  Optional<SubscriptionTrafficState> getTrafficStateBySubscriptionId(
      Long subscriptionId
  );

  SubscriptionTrafficState resetTrafficPeriod(Long subscriptionId);

  void resetAllExpiredPeriods();
}
