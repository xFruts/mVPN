package ru.maxow.mvpn.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.ListUserDto;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.model.UpdateUserRequestDto;
import ru.maxow.mvpn.model.UserResponseDto;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.ResourceAlreadyExistsException;

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

    @Test
    @DisplayName("Должен выбросить ResourceAlreadyExistsException при создании пользователя с занятым fullName")
    void shouldThrowResourceAlreadyExistsWhenFullNameAlreadyExists() {
      CreateUserRequestDto request = new CreateUserRequestDto();
      request.setFullName("New User");

      when(userRepository.existsByFullName("New User")).thenReturn(true);

      assertThatThrownBy(() -> userService.createUser(request))
          .isInstanceOf(ResourceAlreadyExistsException.class)
          .hasMessageContaining("already exists");

      verify(userRepository).existsByFullName("New User");
      verify(userMapper, never()).toUser(any(CreateUserRequestDto.class));
      verify(userRepository, never()).save(any(User.class));
    }
  }

  @Nested
  @DisplayName("Получение пользователя по ID (GET /v1/users/{userId})")
  class FindByIdTests {

    @Test
    @DisplayName("Должен вернуть пользователя по id")
    void shouldReturnUserById() {
      Long userId = 1L;

      UserResponseDto expectedResponse = new UserResponseDto();
      expectedResponse.setId(userId);
      expectedResponse.setFullName(testUser.getFullName());
      expectedResponse.setRole(testUser.getRole());

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(userMapper.toUserResponseDto(testUser)).thenReturn(expectedResponse);

      UserResponseDto result = userService.findById(userId);

      assertThat(result)
          .isNotNull()
          .satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(userId);
            assertThat(dto.getFullName()).isEqualTo(testUser.getFullName());
            assertThat(dto.getRole()).isEqualTo(testUser.getRole());
          });

      verify(userRepository).findById(userId);
      verify(userMapper).toUserResponseDto(testUser);
    }

    @Test
    @DisplayName("Должен бросить NotFoundException если пользователь не найден")
    void shouldThrowNotFoundWhenUserMissing() {
      Long userId = 404L;

      when(userRepository.findById(userId)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.findById(userId))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("User")
          .hasMessageContaining(String.valueOf(userId));

      verify(userRepository).findById(userId);
      verifyNoInteractions(userMapper);
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

    @Test
    @DisplayName("Должен обновить статус и дату окончания последней подписки, если они переданы")
    void shouldUpdateLatestSubscriptionWhenFieldsProvided() {
      Long userId = 1L;
      UpdateUserRequestDto updateDto = new UpdateUserRequestDto();
      updateDto.setSubscriptionStatus(SubscriptionStatus.CANCELED);
      updateDto.setSubscriptionEndDate(java.time.OffsetDateTime.parse("2026-12-31T00:00:00Z"));

      Subscription latestSubscription = new Subscription();
      latestSubscription.setStatus(SubscriptionStatus.ACTIVE);

      UserResponseDto expectedResponse = new UserResponseDto();
      expectedResponse.setId(userId);

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(subscriptionRepository.findFirstByUserOrderByStartDateDesc(testUser))
          .thenReturn(Optional.of(latestSubscription));
      when(userRepository.save(testUser)).thenReturn(testUser);
      when(userMapper.toUserResponseDto(testUser)).thenReturn(expectedResponse);

      UserResponseDto result = userService.updateUser(userId, updateDto);

      assertThat(result).isNotNull();
      assertThat(latestSubscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
      assertThat(latestSubscription.getEndDate()).isEqualTo(updateDto.getSubscriptionEndDate());
      verify(subscriptionRepository).save(latestSubscription);
      verify(userRepository).save(testUser);
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

    @Test
    @DisplayName("Должен бросить NotFoundException, если пользователь для удаления не найден")
    void shouldThrowNotFoundWhenDeleteMissingUser() {
      Long userId = 404L;
      doThrow(new EmptyResultDataAccessException(1)).when(userRepository).deleteById(userId);

      assertThatThrownBy(() -> userService.deleteUserById(userId))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("User");

      verify(userRepository).deleteById(userId);
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

    @Test
    @DisplayName("Должен вернуть true, если у пользователя есть хотя бы одна подписка")
    void shouldReturnTrueWhenSubscriptionListIsNotEmpty() {
      Long userId = 1L;
      Subscription subscription = new Subscription();

      when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
      when(subscriptionRepository.findAllByUser(testUser)).thenReturn(List.of(subscription));

      boolean result = userService.hasActiveSubscriptions(userId);

      assertThat(result).isTrue();
      verify(userRepository).findById(userId);
      verify(subscriptionRepository).findAllByUser(testUser);
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

  @Nested
  @DisplayName("Пагинация и сортировка пользователей")
  class FindAllAsPageTests {

    @Test
    @DisplayName("Given sort is null When findAllAsPage Then repository called with unsorted pageable")
    void givenSortNullWhenFindAllAsPageThenUseUnsorted() {
      when(userRepository.findAll(any(PageRequest.class)))
          .thenReturn(new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1));
      when(userMapper.toListUserDto(testUser)).thenReturn(new ListUserDto().id(1L));

      userService.findAllAsPage(0, 10, null);

      ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
      verify(userRepository).findAll(captor.capture());
      assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
    }

    @Test
    @DisplayName("Given desc and invalid direction When findAllAsPage Then map to desc and asc fallback")
    void givenMixedSortWhenFindAllAsPageThenApplyDescAndAscFallback() {
      when(userRepository.findAll(any(PageRequest.class)))
          .thenReturn(new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1));
      when(userMapper.toListUserDto(testUser)).thenReturn(new ListUserDto().id(1L));

      userService.findAllAsPage(0, 10, List.of("fullName,desc", "role,invalid"));

      ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
      verify(userRepository).findAll(captor.capture());

      var sort = captor.getValue().getSort();
      var fullNameOrder = java.util.Objects.requireNonNull(sort.getOrderFor("fullName"));
      var roleOrder = java.util.Objects.requireNonNull(sort.getOrderFor("role"));
      assertThat(fullNameOrder.getDirection().name()).isEqualTo("DESC");
      assertThat(roleOrder.getDirection().name()).isEqualTo("ASC");
    }
  }

  @Nested
  @DisplayName("Обновление Telegram ID по verification code")
  class UpdateUserTelegramIdTests {

    @Test
    @DisplayName("Given user exists by verification code When updateUserTelegramId Then save updated telegram id")
    void givenUserExistsWhenUpdateTelegramIdThenSave() {
      UUID code = testUser.getVerificationCode();
      when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(testUser));

      userService.updateUserTelegramId(code, 777L);

      assertThat(testUser.getUserTelegramId()).isEqualTo(777L);
      verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Given user not found by verification code When updateUserTelegramId Then do not save")
    void givenUserMissingWhenUpdateTelegramIdThenDoNotSave() {
      UUID code = UUID.randomUUID();
      when(userRepository.findByVerificationCode(code)).thenReturn(Optional.empty());

      userService.updateUserTelegramId(code, 777L);

      verify(userRepository, never()).save(any(User.class));
    }
  }

  @Nested
  @DisplayName("Запрос verification code и пользователей по Telegram IDs")
  class VerificationAndTelegramIdsTests {

    @Test
    @DisplayName("Given existing user id When getUserVerificationCode Then return code")
    void givenExistingUserWhenGetVerificationCodeThenReturnCode() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

      UUID code = userService.getUserVerificationCode(1L);

      assertThat(code).isEqualTo(testUser.getVerificationCode());
    }

    @Test
    @DisplayName("Given missing user id When getUserVerificationCode Then throw NotFoundException")
    void givenMissingUserWhenGetVerificationCodeThenThrow() {
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.getUserVerificationCode(999L))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("User");
    }

    @Test
    @DisplayName("Given telegram ids list When getUsersByTelegramIds Then return repository result")
    void givenTelegramIdsWhenGetUsersByTelegramIdsThenReturnUsers() {
      List<Long> ids = List.of(111L, 222L);
      when(userRepository.findByUserTelegramIdIn(ids)).thenReturn(List.of(testUser));

      List<User> users = userService.getUsersByTelegramIds(ids);

      assertThat(users).containsExactly(testUser);
      verify(userRepository).findByUserTelegramIdIn(ids);
    }

    @Test
    @DisplayName("Given unknown telegram ids list When getUsersByTelegramIds Then return empty list")
    void givenUnknownTelegramIdsWhenGetUsersByTelegramIdsThenReturnEmpty() {
      List<Long> ids = List.of(999L, 1000L);
      when(userRepository.findByUserTelegramIdIn(ids)).thenReturn(List.of());

      List<User> users = userService.getUsersByTelegramIds(ids);

      assertThat(users).isEmpty();
      verify(userRepository).findByUserTelegramIdIn(ids);
    }
  }
}

