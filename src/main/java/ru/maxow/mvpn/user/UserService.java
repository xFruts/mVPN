package ru.maxow.mvpn.user;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing users.
 */
public interface UserService {
  /**
   * Retrieves a paginated list of all users.
   *
   * @param pageable pagination information
   * @return a page of UserResponseDto
   */
  Page<UserResponseDto> findAll(Pageable pageable);

  /**
   * Creates a new user.
   *
   * @param userRequestDto the user request data
   * @return the created UserResponseDto
   */
  UserResponseDto createUser(UserRequestDto userRequestDto);

  /**
   * Updates an existing user.
   *
   * @param id the ID of the user to update
   * @param userRequestDto the updated user data
   * @return the updated UserResponseDto
   */
  UserResponseDto updateUser(Long id, UserRequestDto userRequestDto);

  /**
   * Finds a user by their ID.
   *
   * @param id the ID of the user
   * @return the User entity
   */
  User findUserById(Long id);

  /**
   * Updates the role of a user.
   *
   * @param userId the ID of the user
   * @param userRole the new role for the user
   * @return the updated UserResponseDto
   */
  UserResponseDto updateUserRole(Long userId, String userRole);

  /**
   * Deletes a user by their ID.
   *
   * @param id the ID of the user to delete
   */
  void deleteUserById(Long id);

  /**
   * Checks if a verification code valid.
   *
   * @param code the verification code
   * @return true if the code valid, false otherwise
   */
  boolean checkVerificationCode(UUID code);

  /**
   * Updates the Telegram ID of a user based on their verification code.
   *
   * @param code the verification code
   * @param telegramId the new Telegram ID
   * @return true if the update was successful, false otherwise
   */
  boolean updateUserTelegramId(UUID code, Long telegramId);

  /**
   * Retrieves a list of all regular users (user with role REGULAR).
   *
   * @return a list of User entities
   */
  List<User> getRegularUsers();

  /**
   * Finds a user by their Telegram ID.
   *
   * @param telegramId the Telegram ID of the user
   * @return the User entity
   */
  User findByTelegramId(Long telegramId);
}
