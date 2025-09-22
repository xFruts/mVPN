package ru.maxow.mvpn.user;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserServiceImpl implements UserService {

  UserRepository userRepository;
  UserMapper userMapper;

  @Override
  public Page<UserResponseDto> findAll(Pageable pageable) {
    Page<User> users = userRepository.findAll(pageable);

    return users.map(userMapper::toUserResponseDto);
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

  @Override
  public User findUserById(Long id) {
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
}
