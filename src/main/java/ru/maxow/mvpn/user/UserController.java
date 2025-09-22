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
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
  UserService userService;

  @GetMapping
  public ResponseEntity<Page<UserResponseDto>> getUsers(Pageable pageable) {
    Page<UserResponseDto> users = userService.findAll(pageable);
    return ResponseEntity.ok(users);
  }
  
  @PostMapping
  public ResponseEntity<UserResponseDto> saveUser(
      @RequestBody UserRequestDto userRequestDto) {
    UserResponseDto createdUser = userService.createUser(userRequestDto);
    log.info("User with ID: {} created: ", createdUser.id());
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
  }

  @PutMapping("/{userId}")
  public ResponseEntity<UserResponseDto> updateUser(@PathVariable Long userId, @RequestBody UserRequestDto userRequestDto) {
    UserResponseDto updatedUser = userService.updateUser(userId, userRequestDto);
    log.info("User with ID: {} updated: ", updatedUser.id());
    return ResponseEntity.ok(updatedUser);
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<UserResponseDto> deleteUser(@PathVariable Long userId) {
    userService.deleteUserById(userId);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{userId}/role")
  public ResponseEntity<UserResponseDto> updateUserRole(
      @PathVariable Long userId, @RequestBody UpdateUserRoleRequest request) {
    UserResponseDto updatedUser = userService.updateUserRole(userId, request.role());
    return ResponseEntity.ok(updatedUser);
  }
}
