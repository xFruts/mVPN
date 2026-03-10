package ru.maxow.mvpn.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.maxow.mvpn.model.UserRole;

/**
 * Repository interface for User entity operations.
 */
public interface UserRepository extends JpaRepository<User, Long> {
  /** Find a user by their verification code.
   *
   * @param code the verification code to search for
   * @return an Optional containing the User if found, or empty if not found
   */
  Optional<User> findByVerificationCode(UUID code);

  /** Check if a user exists with the given verification code.
   *
   * @param code the verification code to check
   * @return true if a user with the given code exists, false otherwise
   */
  boolean existsByVerificationCode(UUID code);

  /** Retrieve all users with the role 'REGULAR'.
   *
   * @return a list of users with the 'REGULAR' role
   */
  @Query("SELECT u FROM User u WHERE u.role = 'REGULAR'")
  List<User> getRegularUser();

  /** Find a user by their Telegram ID.
   *
   * @param telegramId the Telegram ID to search for
   * @return an Optional containing the User if found, or empty if not found
   */
  Optional<User> findByUserTelegramId(Long telegramId);

  /** Find all users with a specific role.
   *
   * @param role the role to filter users by
   * @return a list of users with the specified role
   */
  List<User> findAllByRole(UserRole role);

  /** Find users by a list of Telegram IDs.
   *
   * @param ids the list of Telegram IDs to search for
   * @return a list of users whose Telegram IDs are in the provided list
   */
  List<User> findByUserTelegramIdIn(List<Long> ids);
}
