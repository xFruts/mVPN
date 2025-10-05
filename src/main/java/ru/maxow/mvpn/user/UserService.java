package ru.maxow.mvpn.user;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.maxow.mvpn.user.dto.CreateUserRequestDto;
import ru.maxow.mvpn.user.dto.ListUserDto;
import ru.maxow.mvpn.user.dto.UpdateUserRequestDto;
import ru.maxow.mvpn.user.dto.UserResponseDto;

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
  Page<ListUserDto> findAllAsPage(Pageable pageable);

  /**
   * Retrieves a list of all users.
   * Don't use for controllers.
   * For controllers use findAllAsPage method.
   *
   * @return a list of User entities
   */
  List<User> findAll();

  /**
   * Creates a new user.
   *
   * @param dto the data for the new user
   * @return the created UserResponseDto
   */
  UserResponseDto createUser(CreateUserRequestDto dto);

  /**
   * Updates an existing user.
   *
   * @param id  the ID of the user to update
   * @param dto the updated data for the user
   * @return the updated UserResponseDto
   */
  UserResponseDto updateUser(Long id, UpdateUserRequestDto dto);

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
   * @param code       the verification code
   * @param telegramId the new Telegram ID
   */
  void updateUserTelegramId(UUID code, Long telegramId);

  /**
   * Finds a user by their Telegram ID.
   *
   * @param telegramId the Telegram ID of the user
   * @return the User entity
   */
  User findByTelegramId(Long telegramId);

  /**
   * Retrieves users by a list of Telegram IDs.
   *
   * @param ids the list of Telegram IDs
   * @return a list of User entities
   */
  List<User> getUsersByTelegramIds(List<Long> ids);

  /**
   * Retrieves users by their role.
   *
   * @param role the role to filter users by
   * @return a list of User entities with the specified role
   */
  List<User> getUsersByRole(UserRole role);
}
