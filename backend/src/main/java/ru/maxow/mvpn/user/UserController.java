package ru.maxow.mvpn.user;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.UsersApi;
import ru.maxow.mvpn.model.*;

import java.util.List;
import java.util.UUID;


@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController implements UsersApi {

  UserService userService;

  @Override
  public PageListUserDto getUsers(Integer page, Integer size, List<String> sort) {
    return userService.findAllAsPage(page, size, sort);
  }

  @Override
  public UserResponseDto createUser(CreateUserRequestDto createUserRequestDto) {
    UserResponseDto createdUser = userService.createUser(createUserRequestDto);
    log.info("User with ID: {} created: ", createdUser.getId());
    return createdUser;
  }

  @Override
  public UserResponseDto updateUser(Long userId, UpdateUserRequestDto updateUserRequestDto) {
    UserResponseDto updatedUser = userService.updateUser(userId, updateUserRequestDto);
    log.info("User with ID: {} updated: ", updatedUser.getId());
    return updatedUser;
  }

  @Override
  public void deleteUser(Long userId) {
    userService.deleteUserById(userId);
  }

  @Override
  public UserResponseDto updateUserRole(Long userId, UpdateUserRoleRequest updateUserRoleRequest) {
    return userService.updateUserRole(userId, String.valueOf(updateUserRoleRequest.getRole()));
  }

  @Override
  public UUID getUserVerificationCode(Long userId) {
    return userService.getUserVerificationCode(userId);
  }
}
