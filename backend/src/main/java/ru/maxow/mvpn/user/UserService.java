package ru.maxow.mvpn.user;

import java.util.List;
import java.util.UUID;
import ru.maxow.mvpn.model.*;
import ru.maxow.mvpn.subscription.Subscription;

public interface UserService {

  PageListUserDto findAllAsPage(Integer page, Integer size, List<String> sort);

  List<User> findAll();

  UserResponseDto createUser(CreateUserRequestDto dto);

  UserResponseDto updateUser(Long id, UpdateUserRequestDto dto);

  UserResponseDto updateUserRole(Long userId, String userRole);

  void deleteUserById(Long id);

  boolean checkVerificationCode(UUID code);

  void updateUserTelegramId(UUID code, Long telegramId);

  User findByTelegramId(Long telegramId);

  List<User> getUsersByTelegramIds(List<Long> ids);

  List<User> getUsersByRole(UserRole role);

  Subscription findLastUserSubscriptionByUserId(Long userId);

  boolean hasActiveSubscriptions(Long userId);

  UUID getUserVerificationCode(Long userId);
}
