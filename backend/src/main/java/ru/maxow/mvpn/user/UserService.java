package ru.maxow.mvpn.user;

import java.util.List;
import java.util.UUID;
import ru.maxow.mvpn.model.*;

public interface UserService {

  PageListUserDto findAllAsPage(Integer page, Integer size, List<String> sort, String role,
                                       String tariff, String subStatus, String search);

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
