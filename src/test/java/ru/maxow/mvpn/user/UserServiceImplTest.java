package ru.maxow.mvpn.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserRequestDto userRequestDto;
    private UserResponseDto userResponseDto;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFullName("Test User");
        user.setRole(UserRole.USER);

        userRequestDto = new UserRequestDto("Test User", 12345L);
        userResponseDto = new UserResponseDto(1L, "Test User", "key", "USER");
    }

    @Test
    void findAll_shouldReturnPageOfUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(Collections.singletonList(user));
        when(userRepository.findAll(pageable)).thenReturn(userPage);
        when(userMapper.toUserResponseDto(any(User.class))).thenReturn(userResponseDto);

        Page<UserResponseDto> result = userService.findAll(pageable);

        assertFalse(result.isEmpty());
        assertEquals(1, result.getTotalElements());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void createUser_shouldCreateAndReturnUser() {
        when(userMapper.toUser(any(UserRequestDto.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserResponseDto(any(User.class))).thenReturn(userResponseDto);

        UserResponseDto result = userService.createUser(userRequestDto);

        assertNotNull(result);
        assertEquals(userResponseDto.id(), result.id());
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_shouldUpdateAndReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserResponseDto(any(User.class))).thenReturn(userResponseDto);

        UserResponseDto result = userService.updateUser(1L, userRequestDto);

        assertNotNull(result);
        verify(userMapper).updateUserFromUserResponseDto(userRequestDto, user);
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_whenUserNotFound_shouldThrowNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.updateUser(1L, userRequestDto));
    }

    @Test
    void findUserById_shouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.findUserById(1L);

        assertNotNull(result);
        assertEquals(user.getId(), result.getId());
    }

    @Test
    void findUserById_whenUserNotFound_shouldThrowNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.findUserById(1L));
    }

    @Test
    void updateUserRole_shouldUpdateAndReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toUserResponseDto(any(User.class))).thenReturn(userResponseDto);

        UserResponseDto result = userService.updateUserRole(1L, "ADMIN");

        assertNotNull(result);
        assertEquals(UserRole.ADMIN, user.getRole());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserRole_withInvalidRole_shouldThrowBadRequestException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(BadRequestException.class, () -> userService.updateUserRole(1L, "INVALID_ROLE"));
    }

    @Test
    void deleteUserById_shouldDeleteUser() {
        doNothing().when(userRepository).deleteById(1L);

        userService.deleteUserById(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUserById_whenUserNotFound_shouldThrowNotFoundException() {
        doThrow(new org.springframework.dao.EmptyResultDataAccessException(1)).when(userRepository).deleteById(1L);

        assertThrows(NotFoundException.class, () -> userService.deleteUserById(1L));
    }
}

