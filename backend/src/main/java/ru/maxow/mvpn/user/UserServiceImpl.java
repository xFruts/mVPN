package ru.maxow.mvpn.user;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.subscription.SubscriptionRequestDto;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.subscription.SubscriptionType;
import ru.maxow.mvpn.user.dto.CreateUserRequestDto;
import ru.maxow.mvpn.user.dto.ListUserDto;
import ru.maxow.mvpn.user.dto.UpdateUserRequestDto;
import ru.maxow.mvpn.user.dto.UserResponseDto;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

/**
 * Service implementation for managing users.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

  UserRepository userRepository;
  SubscriptionRepository subscriptionRepository;
  UserMapper userMapper;
  SubscriptionService subscriptionService;

  @Override
  @Transactional(readOnly = true)
  public Page<ListUserDto> findAllAsPage(Pageable pageable) {
    Page<User> users = userRepository.findAll(pageable);

    return users.map(userMapper::toListUserDto);
  }

  @Override
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Override
  @Transactional
  public UserResponseDto createUser(CreateUserRequestDto dto) {
    User user = userMapper.toUser(dto);

    userRepository.save(user);
    log.info("User with id: {} created successfully", user.getId());

    createSubscriptionForUser(user, dto.subscriptionType());

    return userMapper.toUserResponseDto(user);
  }

  private void createSubscriptionForUser(User user, SubscriptionType type) {
    if (type != null) {
      SubscriptionRequestDto subscriptionRequestDto = new SubscriptionRequestDto(type);
      subscriptionService.createSubscription(user.getId(), subscriptionRequestDto);
      log.info("Subscription of type {} created for user with id: {}",
          type, user.getId());
    }
  }

  @Override
  @Transactional
  public UserResponseDto updateUser(Long id, UpdateUserRequestDto dto) {
    User existingUser = findUserById(id);

    if (dto.fullName() != null && !dto.fullName().isBlank()) {
      existingUser.setFullName(dto.fullName());
    }
    if (dto.userTelegramId() != null) {
      existingUser.setUserTelegramId(dto.userTelegramId());
    }
    if (dto.role() != null) {
      existingUser.setRole(dto.role());
    }

    updateUserSubscription(existingUser, dto);

    User updatedUser = userRepository.save(existingUser);
    log.info("User with ID: {} and their subscription updated successfully", id);
    return userMapper.toUserResponseDto(updatedUser);
  }

  private void updateUserSubscription(User user, UpdateUserRequestDto dto) {
    subscriptionRepository.findFirstByUserOrderByStartDateDesc(user)
        .ifPresent(subscription -> {
          if (dto.subscriptionType() != null) {
            subscription.setType(dto.subscriptionType());
          }
          if (dto.subscriptionStatus() != null) {
            subscription.setStatus(dto.subscriptionStatus());
          }
          if (dto.subscriptionEndDate() != null) {
            subscription.setEndDate(dto.subscriptionEndDate());
          }
          subscriptionRepository.save(subscription);
          log.info("Subscription for user with ID: {} updated successfully", user.getId());
        });
  }

  private User findUserById(Long id) {
    return userRepository.findById(id).orElseThrow(() ->
        new NotFoundException("User", id));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasActiveSubscriptions(Long userId) {
    User user = findUserById(userId);
    return !subscriptionRepository.findAllByUser(user).isEmpty();
  }

  @Override
  public UserResponseDto updateUserRole(Long userId, String userRole) {
    User existingUser = findUserById(userId);
    UserRole newRole;

    try {
      newRole = UserRole.valueOf(userRole.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid user role:" + userRole);
    }

    existingUser.setRole(newRole);
    User updatedUser = userRepository.save(existingUser);

    log.info("Role for user with ID: {} updated successfully to: {}", userId, newRole);
    return userMapper.toUserResponseDto(updatedUser);
  }

  @Override
  @Transactional
  public void deleteUserById(Long id) {
    try {
      userRepository.deleteById(id);
      log.info("User with ID: {} deleted successfully", id);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("User", id);
    }
  }

  @Override
  public boolean checkVerificationCode(UUID code) {
    return userRepository.existsByVerificationCode(code);
  }

  @Override
  public void updateUserTelegramId(UUID code, Long telegramId) {
    User existingUser = userRepository.findByVerificationCode(code)
            .orElse(null);

    if (existingUser == null) {
      return;
    }

    existingUser.setUserTelegramId(telegramId);
    userRepository.save(existingUser);
    log.info("Telegram ID for user with verification code: {} updated successfully", code);
  }

  @Override
  public User findByTelegramId(Long telegramId) {
    return userRepository.findByUserTelegramId(telegramId).orElse(null);
  }

  @Override
  public List<User> getUsersByTelegramIds(List<Long> ids) {
    return userRepository.findByUserTelegramIdIn(ids);
  }

  @Override
  public List<User> getUsersByRole(UserRole role) {
    return userRepository.findAllByRole(role);
  }

  @Override
  public UUID getUserVerificationCode(Long userId) {
    User user = findUserById(userId);
    return user.getVerificationCode();
  }
}
