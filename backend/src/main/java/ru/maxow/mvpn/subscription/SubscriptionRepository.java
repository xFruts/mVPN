package ru.maxow.mvpn.subscription;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.maxow.mvpn.user.User;

/**
 * Repository interface for managing Subscription entities.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
  /**
   * Checks if a subscription exists for a given user and subscription type.
   *
   * @param user the user to check
   * @param subscriptionType the type of subscription to check
   * @return true if a subscription exists, false otherwise
   */
  boolean existsByUserAndType(User user, SubscriptionType subscriptionType);

  /**
   * Finds all subscriptions associated with a specific user ID.
   *
   * @param userId the ID of the user
   * @return a list of subscriptions for the user
   */
  List<Subscription> findByUser_Id(Long userId);

  /**
   * Finds the most recent subscription for a given user based on the start date.
   *
   * @param user the user to find the subscription for
   * @return an Optional containing the most recent subscription if found, or empty if not found
   */
  Optional<Subscription> findFirstByUserOrderByStartDateDesc(User user);

  List<Subscription> findAllByUser(User user);
}
