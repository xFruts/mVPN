package ru.maxow.mvpn.subscription.traffic;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionServerTrafficRepository
    extends JpaRepository<SubscriptionServerTraffic, Long> {

  List<SubscriptionServerTraffic> findBySubscriptionId(Long subscriptionId);

  Optional<SubscriptionServerTraffic> findBySubscriptionIdAndServerId(Long subscriptionId, Long serverId);

  void deleteBySubscriptionId(Long subscriptionId);
}
