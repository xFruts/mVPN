package ru.maxow.mvpn.subscription.traffic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubscriptionTrafficStateRepository extends JpaRepository<SubscriptionTrafficState, Long> {

  Optional<SubscriptionTrafficState> findBySubscriptionId(Long subscriptionId);
}
