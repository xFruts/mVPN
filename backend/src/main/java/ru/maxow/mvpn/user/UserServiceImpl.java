package ru.maxow.mvpn.user;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.*;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.ResourceAlreadyExistsException;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

  UserRepository userRepository;
  SubscriptionRepository subscriptionRepository;
  UserMapper userMapper;

  @Override
  @Transactional(readOnly = true)
  public PageListUserDto findAllAsPage(Integer page, Integer size, List<String> sort) {
    Sort sorting = (sort == null || sort.isEmpty())
        ? Sort.unsorted()
        : Sort.by(sort.stream().map(s -> {
            String[] parts = s.split(",");
            return parts.length == 2 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Order.desc(parts[0])
                : Sort.Order.asc(parts[0]);
          }).toList());

    Page<User> users = userRepository.findAll(PageRequest.of(page, size, sorting));

    return new PageListUserDto()
        .content(users.getContent().stream().map(userMapper::toListUserDto).toList())
        .totalElements(users.getTotalElements())
        .totalPages(users.getTotalPages())
        .size(users.getSize())
        .number(users.getNumber());
  }

  @Override
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponseDto findById(Long id) {
    return userRepository.findById(id)
        .map(userMapper::toUserResponseDto)
        .orElseThrow(() -> new NotFoundException("User", id));
  }

  @Override
  @Transactional
  public UserResponseDto createUser(CreateUserRequestDto dto) {
    if (userRepository.existsByFullName(dto.getFullName())) {
      throw new ResourceAlreadyExistsException(
          "User with full name: " + dto.getFullName() + " already exists");
    }

    User user = userMapper.toUser(dto);

    userRepository.save(user);
    log.info("User with id: {} created successfully", user.getId());

    return userMapper.toUserResponseDto(user);
  }

  @Override
  @Transactional
  public UserResponseDto updateUser(Long id, UpdateUserRequestDto dto) {
    User existingUser = findUserById(id);

    if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
      existingUser.setFullName(dto.getFullName());
    }
    if (dto.getUserTelegramId() != null) {
      existingUser.setUserTelegramId(dto.getUserTelegramId());
    }
    if (dto.getRole() != null) {
      existingUser.setRole(dto.getRole());
    }

    updateUserSubscription(existingUser, dto);

    User updatedUser = userRepository.save(existingUser);
    log.info("User with ID: {} and their subscription updated successfully", id);
    return userMapper.toUserResponseDto(updatedUser);
  }

  private void updateUserSubscription(User user, UpdateUserRequestDto dto) {
    subscriptionRepository.findFirstByUserOrderByStartDateDesc(user)
        .ifPresent(subscription -> {
          if (dto.getSubscriptionStatus() != null) {
            subscription.setStatus(dto.getSubscriptionStatus());
          }
          if (dto.getSubscriptionEndDate() != null) {
            subscription.setEndDate(dto.getSubscriptionEndDate());
          }
          subscriptionRepository.save(subscription);
          log.info("Subscription for user with ID: {} updated successfully", user.getId());
        });
  } //TODO: refactor with SubscriptionService

  private User findUserById(Long id) {
    return userRepository.findById(id).orElseThrow(() ->
        new NotFoundException("User", id));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAnySubscriptions(Long userId) {
    User user = findUserById(userId);
    return subscriptionRepository.findFirstByUserOrderByStartDateDesc(user).isPresent();
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
