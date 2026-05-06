package ru.maxow.mvpn.subscription;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.maxow.mvpn.user.User;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

  List<Subscription> findByUser_Id(Long userId);

  Optional<Subscription> findFirstByUser_IdOrderByStartDateDesc(Long userId);

  Optional<Subscription> findFirstByUserOrderByStartDateDesc(User user);

  @EntityGraph(attributePaths = "tariff")
  Optional<Subscription> findFirstWithTariffByUserOrderByStartDateDesc(User user);

  @EntityGraph(attributePaths = {"tariff", "tariff.servers"})
  Optional<Subscription> findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(Long userId);

  List<Subscription> findAllByUser(User user);
}
