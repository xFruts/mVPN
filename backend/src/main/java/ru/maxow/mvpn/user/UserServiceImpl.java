package ru.maxow.mvpn.user;

import static ru.maxow.mvpn.user.UserSpecifications.hasRole;
import static ru.maxow.mvpn.user.UserSpecifications.hasSubscriptionStatus;
import static ru.maxow.mvpn.user.UserSpecifications.nameContains;
import static ru.maxow.mvpn.util.PaginationUtils.parseSorting;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.PageListUserDto;
import ru.maxow.mvpn.model.ShortListUserDto;
import ru.maxow.mvpn.model.UpdateUserRequestDto;
import ru.maxow.mvpn.model.UserResponseDto;
import ru.maxow.mvpn.model.UserRole;
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
  public PageListUserDto findAllAsPage(Integer page, Integer size, List<String> sort,
                                       String role, String subStatus, String search) {
    Specification<User> spec = Specification.where(hasRole(role))
        .and(hasSubscriptionStatus(subStatus))
        .and(nameContains(search));

    Page<User> users = userRepository.findAll(spec, PageRequest.of(page, size, parseSorting(sort)));

    return new PageListUserDto()
        .content(users.getContent().stream().map(userMapper::toListUserDto).toList())
        .totalElements(users.getTotalElements())
        .totalPages(users.getTotalPages())
        .number(users.getNumber())
        .size(users.getSize());
  }

  @Override
  @Transactional(readOnly = true)
  public List<ShortListUserDto> findAllAsList() {
    return userRepository.findAll().stream()
        .map(userMapper::toShortListUserDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponseDto findById(Long id) {
    return userMapper.toUserResponseDto(findUserById(id));
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
    log.info("User created: id={}", user.getId());
    return userMapper.toUserResponseDto(user);
  }

  @Override
  @Transactional
  public UserResponseDto updateUser(Long id, UpdateUserRequestDto dto) {
    User existingUser = findUserById(id);

    if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
      String newFullName = dto.getFullName().trim();
      if (!newFullName.equals(existingUser.getFullName())
          && userRepository.existsByFullName(newFullName)) {
        throw new ResourceAlreadyExistsException(
            "User with full name: " + newFullName + " already exists");
      }
      existingUser.setFullName(newFullName);
    }
    if (dto.getUserTelegramId() != null) {
      existingUser.setUserTelegramId(dto.getUserTelegramId());
    }
    if (dto.getRole() != null) {
      existingUser.setRole(dto.getRole());
    }

    updateUserSubscription(existingUser, dto);

    User updatedUser = userRepository.save(existingUser);
    log.info("User updated: id={}", id);
    return userMapper.toUserResponseDto(updatedUser);
  }

  private void updateUserSubscription(User user, UpdateUserRequestDto dto) {
    if (dto.getSubscriptionStatus() == null && dto.getSubscriptionEndDate() == null) {
      return;
    }

    subscriptionRepository.findFirstByUserOrderByStartDateDesc(user)
        .ifPresent(subscription -> {
          if (dto.getSubscriptionStatus() != null) {
            subscription.setStatus(dto.getSubscriptionStatus());
          }
          if (dto.getSubscriptionEndDate() != null) {
            subscription.setEndDate(dto.getSubscriptionEndDate());
          }
          subscriptionRepository.save(subscription);
          log.info("Latest subscription updated for user id={}", user.getId());
        });
  }

  private User findUserById(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("User", id));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasAnySubscriptions(Long userId) {
    User user = findUserById(userId);
    return subscriptionRepository.findFirstByUserOrderByStartDateDesc(user).isPresent();
  }

  @Override
  @Transactional
  public UserResponseDto updateUserRole(Long userId, String userRole) {
    if (userRole == null || userRole.isBlank()) {
      throw new BadRequestException("Invalid user role:" + userRole);
    }

    User existingUser = findUserById(userId);
    final UserRole newRole;
    try {
      newRole = UserRole.valueOf(userRole.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid user role:" + userRole);
    }

    existingUser.setRole(newRole);
    User updatedUser = userRepository.save(existingUser);
    log.info("User role updated: id={}, role={}", userId, newRole);
    return userMapper.toUserResponseDto(updatedUser);
  }

  @Override
  @Transactional
  public void deleteUserById(Long id) {
    if (!userRepository.existsById(id)) {
      throw new NotFoundException("User", id);
    }
    userRepository.deleteById(id);
    log.info("User deleted: id={}", id);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean checkVerificationCode(UUID code) {
    return userRepository.existsByVerificationCode(code);
  }

  @Override
  @Transactional
  public void updateUserTelegramId(UUID code, Long telegramId) {
    userRepository.findByVerificationCode(code).ifPresent(user -> {
      user.setUserTelegramId(telegramId);
      userRepository.save(user);
      log.info("Telegram id bound for user id={}", user.getId());
    });
  }

  @Override
  @Transactional(readOnly = true)
  public User findByTelegramId(Long telegramId) {
    return userRepository.findByUserTelegramId(telegramId).orElse(null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getUsersByTelegramIds(List<Long> ids) {
    return userRepository.findByUserTelegramIdIn(ids);
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> getUsersByRole(UserRole role) {
    return userRepository.findAllByRole(role);
  }

  @Override
  @Transactional(readOnly = true)
  public UUID getUserVerificationCode(Long userId) {
    return findUserById(userId).getVerificationCode();
  }
}
