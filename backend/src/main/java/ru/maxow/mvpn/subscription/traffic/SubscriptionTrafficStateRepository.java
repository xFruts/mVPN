package ru.maxow.mvpn.subscription.traffic;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionTrafficStateRepository
    extends JpaRepository<SubscriptionTrafficState, Long> {

  Optional<SubscriptionTrafficState> findBySubscriptionId(Long subscriptionId);
}
