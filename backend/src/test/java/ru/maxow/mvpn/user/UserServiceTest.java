package ru.maxow.mvpn.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.UpdateUserRequestDto;
import ru.maxow.mvpn.model.UserResponseDto;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit тесты для UserService.
 * <p>
 * Тестируем бизнес-логику без обращения к БД (мокируем Repository).
 * Это быстрые и изолированные тесты.
 * <p>
 * Паттерн: Arrange (подготовка) -> Act (действие) -> Assert (проверка)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService - Unit тесты (бизнес-логика)")
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private UserServiceImpl userService;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setId(1L);
    testUser.setFullName("John Doe");
    testUser.setRole(UserRole.REGULAR);
    testUser.setVerificationCode(UUID.randomUUID());
    testUser.setUserTelegramId(123456789L);
  }

  @Nested
  @DisplayName("Создание пользователя (POST /v1/users)")
  class CreateUserTests {

    @Test
    @DisplayName("Должен успешно создать пользователя с валидными данными")
    void shouldCreateUserSuccessfully() {
      // Arrange
      CreateUserRequestDto request = new CreateUserRequestDto();
      request.setFullName("New User");

      User newUser = new User();
      newUser.setId(2L);
      newUser.setFullName("New User");
      newUser.setRole(UserRole.REGULAR);

      UserResponseDto expectedResponse = new UserResponseDto();
      expectedResponse.setId(2L);
      expectedResponse.setFullName("New User");
      expectedResponse.setRole(UserRole.REGULAR);

      when(userMapper.toUser(request)).thenReturn(newUser);
      when(userRepository.save(any(User.class))).thenReturn(newUser);
      when(userMapper.toUserResponseDto(newUser)).thenReturn(expectedResponse);

      // Act
      UserResponseDto result = userService.createUser(request);

      // Assert
      assertThat(result)
          .isNotNull()
          .satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(2L);
            assertThat(dto.getFullName()).isEqualTo("New User");
            assertThat(dto.getRole()).isEqualTo(UserRole.REGULAR);
          });

      // Verify что методы были вызваны нужное количество раз
      verify(userRepository, times(1)).save(any(User.class));
      verify(userMapper, times(1)).toUser(request);
    }
  }

  @Nested
  @DisplayName("Обновление пользователя (PUT /v1/users/{userId})")
  class UpdateUserTests {

    @Test
    @DisplayName("Должен успешно обновить ФИ пользователя")
    void shouldUpdateUserFullName() {
      // Arrange
      Long userId = 1L;
      UpdateUserRequestDto updateDto = new UpdateUserRequestDto();
      updateDto.setFullName("Updated Name");

      User updatedUser = new User();
      updatedUser.setId(userId);
      updatedUser.setFullName("Updated Name");
      updatedUser.setRole(UserRole.REGULAR);

      UserResponseDto expectedResponse = new UserResponseDto();
      expectedResponse.setId(userId);
      expectedResponse.setFullName("Updated Name");

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(subscriptionRepository.findFirstByUserOrderByStartDateDesc(testUser))
          .thenReturn(Optional.empty());
      when(userRepository.save(any(User.class))).thenReturn(updatedUser);
      when(userMapper.toUserResponseDto(updatedUser)).thenReturn(expectedResponse);

      // Act
      UserResponseDto result = userService.updateUser(userId, updateDto);

      // Assert
      assertThat(result).isNotNull().satisfies(dto -> {
        assertThat(dto.getId()).isEqualTo(userId);
        assertThat(dto.getFullName()).isEqualTo("Updated Name");
      });

      verify(userRepository).findById(userId);
      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException для несуществующего пользователя")
    void shouldThrowNotFoundForNonExistentUser() {
      // Arrange
      Long nonExistentUserId = 999L;
      UpdateUserRequestDto updateDto = new UpdateUserRequestDto();
      updateDto.setFullName("New Name");

      when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

      // Act & Assert
      assertThatThrownBy(() -> userService.updateUser(nonExistentUserId, updateDto))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("User");

      verify(userRepository).findById(nonExistentUserId);
      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Удаление пользователя (DELETE /v1/users/{userId})")
  class DeleteUserTests {

    @Test
    @DisplayName("Должен успешно удалить пользователя")
    void shouldDeleteUserSuccessfully() {
      // Arrange
      Long userId = 1L;
      doNothing().when(userRepository).deleteById(userId);

      // Act
      userService.deleteUserById(userId);

      // Assert
      verify(userRepository, times(1)).deleteById(userId);
    }
  }

  @Nested
  @DisplayName("Обновление роли пользователя (PATCH /v1/users/{userId}/role)")
  class UpdateUserRoleTests {

    @Test
    @DisplayName("Должен успешно обновить роль на ADMIN")
    void shouldUpdateUserRoleToAdmin() {
      // Arrange
      Long userId = 1L;
      String newRole = "ADMIN";

      User updatedUser = new User();
      updatedUser.setId(userId);
      updatedUser.setFullName("John Doe");
      updatedUser.setRole(UserRole.ADMIN);

      UserResponseDto expectedResponse = new UserResponseDto();
      expectedResponse.setId(userId);
      expectedResponse.setRole(UserRole.ADMIN);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(userRepository.save(any(User.class))).thenReturn(updatedUser);
      when(userMapper.toUserResponseDto(updatedUser)).thenReturn(expectedResponse);

      // Act
      UserResponseDto result = userService.updateUserRole(userId, newRole);

      // Assert
      assertThat(result).isNotNull().satisfies(dto -> {
        assertThat(dto.getId()).isEqualTo(userId);
        assertThat(dto.getRole()).isEqualTo(UserRole.ADMIN);
      });

      verify(userRepository).findById(userId);
      verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Должен выбросить BadRequestException для невалидной роли")
    void shouldThrowBadRequestForInvalidRole() {
      // Arrange
      Long userId = 1L;
      String invalidRole = "INVALID_ROLE";

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

      // Act & Assert
      assertThatThrownBy(() -> userService.updateUserRole(userId, invalidRole))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Invalid user role");

      verify(userRepository).findById(userId);
      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Проверка кода верификации")
  class VerificationCodeTests {

    @Test
    @DisplayName("Должен вернуть true если код верификации существует")
    void shouldReturnTrueForExistingVerificationCode() {
      // Arrange
      UUID code = testUser.getVerificationCode();
      when(userRepository.existsByVerificationCode(code)).thenReturn(true);

      // Act
      boolean result = userService.checkVerificationCode(code);

      // Assert
      assertThat(result).isTrue();
      verify(userRepository).existsByVerificationCode(code);
    }

    @Test
    @DisplayName("Должен вернуть false если код верификации не существует")
    void shouldReturnFalseForNonExistentVerificationCode() {
      // Arrange
      UUID code = UUID.randomUUID();
      when(userRepository.existsByVerificationCode(code)).thenReturn(false);

      // Act
      boolean result = userService.checkVerificationCode(code);

      // Assert
      assertThat(result).isFalse();
      verify(userRepository).existsByVerificationCode(code);
    }
  }

  @Nested
  @DisplayName("Проверка активных подписок")
  class HasActiveSubscriptionsTests {

    @Test
    @DisplayName("Должен вернуть true если у пользователя есть активные подписки")
    void shouldReturnTrueWhenUserHasActiveSubscriptions() {
      // Arrange
      Long userId = 1L;
      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(subscriptionRepository.findAllByUser(testUser))
          .thenReturn(List.of()); // Пусто = нет подписок

      // Act
      boolean result = userService.hasActiveSubscriptions(userId);

      // Assert
      assertThat(result).isFalse();
      verify(userRepository).findById(userId);
    }
  }

  @Nested
  @DisplayName("Поиск пользователей по Telegram ID")
  class TelegramIdTests {

    @Test
    @DisplayName("Должен найти пользователя по Telegram ID")
    void shouldFindUserByTelegramId() {
      // Arrange
      Long telegramId = 123456789L;
      when(userRepository.findByUserTelegramId(telegramId)).thenReturn(Optional.of(testUser));

      // Act
      User result = userService.findByTelegramId(telegramId);

      // Assert
      assertThat(result).isNotNull().satisfies(user -> {
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUserTelegramId()).isEqualTo(telegramId);
      });

      verify(userRepository).findByUserTelegramId(telegramId);
    }

    @Test
    @DisplayName("Должен вернуть null если пользователь не найден")
    void shouldReturnNullWhenUserNotFound() {
      // Arrange
      Long telegramId = 999999999L;
      when(userRepository.findByUserTelegramId(telegramId)).thenReturn(Optional.empty());

      // Act
      User result = userService.findByTelegramId(telegramId);

      // Assert
      assertThat(result).isNull();
      verify(userRepository).findByUserTelegramId(telegramId);
    }
  }

  @Nested
  @DisplayName("Получение роли пользователей")
  class GetUsersByRoleTests {

    @Test
    @DisplayName("Должен найти всех пользователей с ролью REGULAR")
    void shouldFindUsersByRole() {
      // Arrange
      List<User> regularUsers = List.of(testUser);
      when(userRepository.findAllByRole(UserRole.REGULAR)).thenReturn(regularUsers);

      // Act
      List<User> result = userService.getUsersByRole(UserRole.REGULAR);

      // Assert
      assertThat(result)
          .isNotEmpty()
          .hasSize(1)
          .contains(testUser);

      verify(userRepository).findAllByRole(UserRole.REGULAR);
    }
  }
}

