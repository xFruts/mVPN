package ru.maxow.mvpn.user;

import jakarta.transaction.Transactional;
import java.io.InputStream;
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
import org.springframework.web.multipart.MultipartFile;
import ru.maxow.mvpn.minio.MinioService;
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
  UserMapper userMapper;
  MinioService minioService;

  @Override
  public Page<UserResponseDto> findAllAsPage(Pageable pageable) {
    Page<User> users = userRepository.findAll(pageable);

    return users.map(userMapper::toUserResponseDto);
  }

  @Override
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Override
  public UserResponseDto createUser(UserRequestDto userRequestDto) {
    User user = userMapper.toUser(userRequestDto);

    userRepository.save(user);
    log.info("User with id: {} created successfully", user.getId());
    return userMapper.toUserResponseDto(user);
  }

  @Override
  public UserResponseDto updateUser(Long id, UserRequestDto userRequestDto) {
    User existingUser = findUserById(id);
    userMapper.updateUserFromUserResponseDto(userRequestDto, existingUser);

    User updatedUser = userRepository.save(existingUser);

    log.info("User with ID: {} updated successfully", id);
    return userMapper.toUserResponseDto(updatedUser);
  }

  private User findUserById(Long id) {
    return userRepository.findById(id).orElseThrow(() ->
        new NotFoundException("User", id));
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
  @Transactional
  public void attachConfigFile(Long userId, MultipartFile file) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User", userId));

    String filePath = minioService.uploadFile(file, userId);

    user.setConfigFilePath(filePath);

    userRepository.save(user);
  }

  @Override
  public InputStream downloadConfigFile(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User", userId));

    if (user.getConfigFilePath() == null || user.getConfigFilePath().isEmpty()) {
      throw new NotFoundException("Config file for user", userId);
    }

    return minioService.downloadFile(user.getConfigFilePath());
  }

  @Override
  @Transactional
  public void deleteConfigFile(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("User", userId));

    String filePath = user.getConfigFilePath();
    if (filePath != null && !filePath.isEmpty()) {
      minioService.deleteFile(filePath);
      user.setConfigFilePath(null);
      userRepository.save(user);
      log.info("Config file for user with ID: {} deleted successfully", userId);
    } else {
      throw new NotFoundException("Config file for user", userId);
    }
  }

  @Override
  public List<User> getUsersByTelegramIds(List<Long> ids) {
    return userRepository.findByUserTelegramIdIn(ids);
  }

  @Override
  public List<User> getUsersByRole(UserRole role) {
    return userRepository.findAllByRole(role);
  }
}
