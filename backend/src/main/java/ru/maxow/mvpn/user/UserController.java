package ru.maxow.mvpn.user;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.UsersApi;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.PageListUserDto;
import ru.maxow.mvpn.model.ShortListUserDto;
import ru.maxow.mvpn.model.UpdateUserRequestDto;
import ru.maxow.mvpn.model.UpdateUserRoleRequest;
import ru.maxow.mvpn.model.UserResponseDto;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController implements UsersApi {

  UserService userService;

  @Override
  public PageListUserDto getUsers(Integer page, Integer size, List<String> sort,
                                  String role, String subStatus, String search) {
    return userService.findAllAsPage(page, size, sort, role, subStatus, search);
  }

  @Override
  public List<ShortListUserDto> getUsersList() {
    return userService.findAllAsList();
  }

  @Override
  public UserResponseDto createUser(CreateUserRequestDto createUserRequestDto) {
    UserResponseDto createdUser = userService.createUser(createUserRequestDto);
    log.info("User created: id={}", createdUser.getId());
    return createdUser;
  }

  @Override
  public UserResponseDto updateUser(Long userId, UpdateUserRequestDto updateUserRequestDto) {
    UserResponseDto updatedUser = userService.updateUser(userId, updateUserRequestDto);
    log.info("User updated: id={}", updatedUser.getId());
    return updatedUser;
  }

  @Override
  public void deleteUser(Long userId) {
    userService.deleteUserById(userId);
  }

  @Override
  public UserResponseDto getUserById(Long userId) {
    return userService.findById(userId);
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
