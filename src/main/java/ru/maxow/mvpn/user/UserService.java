package ru.maxow.mvpn.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
  Page<UserResponseDto> findAll(Pageable pageable);
  UserResponseDto createUser(UserRequestDto userRequestDto);
  UserResponseDto updateUser(Long id, UserRequestDto userRequestDto);
  User findUserById(Long id);
  UserResponseDto updateUserRole(Long userId, String userRole);
  void deleteUserById(Long id);
}
