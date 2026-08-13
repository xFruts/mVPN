package ru.maxow.mvpn.user;

import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.PageListUserDto;
import ru.maxow.mvpn.model.ShortListUserDto;
import ru.maxow.mvpn.model.UpdateUserRequestDto;
import ru.maxow.mvpn.model.UserResponseDto;
import ru.maxow.mvpn.model.UserRole;

public interface UserService {

  PageListUserDto findAllAsPage(Integer page, Integer size, List<String> sort,
                                String role, String subStatus, String search);

  @Transactional(readOnly = true)
  List<ShortListUserDto> findAllAsList();

  List<User> findAll();

  UserResponseDto findById(Long id);

  UserResponseDto createUser(CreateUserRequestDto dto);

  UserResponseDto updateUser(Long id, UpdateUserRequestDto dto);

  UserResponseDto updateUserRole(Long userId, String userRole);

  void deleteUserById(Long id);

  boolean checkVerificationCode(UUID code);

  void updateUserTelegramId(UUID code, Long telegramId);

  User findByTelegramId(Long telegramId);

  List<User> getUsersByTelegramIds(List<Long> ids);

  List<User> getUsersByRole(UserRole role);

  boolean hasAnySubscriptions(Long userId);

  UUID getUserVerificationCode(Long userId);
}
