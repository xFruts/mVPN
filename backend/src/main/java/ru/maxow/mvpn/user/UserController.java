package ru.maxow.mvpn.user;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.user.dto.CreateUserRequestDto;
import ru.maxow.mvpn.user.dto.ListUserDto;
import ru.maxow.mvpn.user.dto.UpdateUserRequestDto;
import ru.maxow.mvpn.user.dto.UpdateUserRoleRequest;
import ru.maxow.mvpn.user.dto.UserResponseDto;

import java.util.UUID;


/**
 * REST controller for managing users.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
  UserService userService;

  /**
   * Retrieves a paginated list of users.
   *
   * @param pageable pagination information
   * @return a page of UserResponseDto
   */
  @GetMapping
  public ResponseEntity<Page<ListUserDto>> getUsers(Pageable pageable) {
    Page<ListUserDto> users = userService.findAllAsPage(pageable);
    return ResponseEntity.ok(users);
  }

  /**
   * Saves a new user.
   *
   * @param dto the user data to save
   * @return the created user
   */
  @PostMapping
  public ResponseEntity<UserResponseDto> saveUser(
      @RequestBody CreateUserRequestDto dto) {
    UserResponseDto createdUser = userService.createUser(dto);
    log.info("User with ID: {} created: ", createdUser.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  @PutMapping("/{userId}")
  public ResponseEntity<UserResponseDto> updateUser(
      @PathVariable Long userId, @RequestBody UpdateUserRequestDto dto) {
    UserResponseDto updatedUser = userService.updateUser(userId, dto);
    log.info("User with ID: {} updated: ", updatedUser.id());
    return ResponseEntity.ok(updatedUser);
  }

  /**
   * Deletes a user by ID.
   *
   * @param userId the ID of the user to delete
   * @return a response entity with no content
   */
  @DeleteMapping("/{userId}")
  public ResponseEntity<UserResponseDto> deleteUser(@PathVariable Long userId) {
    userService.deleteUserById(userId);
    return ResponseEntity.noContent().build();
  }

  /**
   * Updates the role of a user.
   *
   * @param userId the ID of the user to update
   * @param request the new role data
   * @return the updated user
   */
  @PatchMapping("/{userId}/role")
  public ResponseEntity<UserResponseDto> updateUserRole(
      @PathVariable Long userId, @RequestBody UpdateUserRoleRequest request) {
    UserResponseDto updatedUser = userService.updateUserRole(userId, request.role());
    return ResponseEntity.ok(updatedUser);
  }

  @GetMapping("/{userId}/code")
  public ResponseEntity<UUID> getVerificationCode(@PathVariable Long userId) {
    return ResponseEntity.ok(userService.getUserVerificationCode(userId));
  }
}
