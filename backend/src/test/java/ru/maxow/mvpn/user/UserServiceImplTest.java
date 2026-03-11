package ru.maxow.mvpn.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.UpdateUserRequestDto;
import ru.maxow.mvpn.model.UserResponseDto;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

@SpringBootTest(classes = UserServiceImpl.class)
@DisplayName("UserServiceImpl - Spring Test with MockitoBean")
class UserServiceImplTest {

  @Autowired
  private UserServiceImpl userService;

  @MockitoBean
  private UserRepository userRepository;

  @MockitoBean
  private SubscriptionRepository subscriptionRepository;

  @MockitoBean
  private UserMapper userMapper;

  @Test
  @DisplayName("createUser: создает пользователя через mapper + repository")
  void createUser_success() {
    CreateUserRequestDto request = new CreateUserRequestDto();
    request.setFullName("John Doe");
    request.setRole(UserRole.REGULAR);

    User user = new User();
    user.setId(1L);
    user.setFullName("John Doe");
    user.setRole(UserRole.REGULAR);

    UserResponseDto response = new UserResponseDto();
    response.setId(1L);
    response.setFullName("John Doe");
    response.setRole(UserRole.REGULAR);

    when(userMapper.toUser(request)).thenReturn(user);
    when(userRepository.save(any(User.class))).thenReturn(user);
    when(userMapper.toUserResponseDto(user)).thenReturn(response);

    UserResponseDto result = userService.createUser(request);

    assertThat(result.getId()).isEqualTo(1L);
    assertThat(result.getRole()).isEqualTo(UserRole.REGULAR);
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("updateUser: обновляет fullName и сохраняет")
  void updateUser_success() {
    Long userId = 10L;
    User existing = new User();
    existing.setId(userId);
    existing.setFullName("Old");
    existing.setRole(UserRole.REGULAR);

    UpdateUserRequestDto dto = new UpdateUserRequestDto();
    dto.setFullName("New Name");

    UserResponseDto response = new UserResponseDto();
    response.setId(userId);
    response.setFullName("New Name");
    response.setRole(UserRole.REGULAR);

    when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
    when(subscriptionRepository.findFirstByUserOrderByStartDateDesc(existing)).thenReturn(Optional.empty());
    when(userRepository.save(existing)).thenReturn(existing);
    when(userMapper.toUserResponseDto(existing)).thenReturn(response);

    UserResponseDto result = userService.updateUser(userId, dto);

    assertThat(result.getFullName()).isEqualTo("New Name");
    verify(userRepository).save(existing);
  }

  @Test
  @DisplayName("updateUserRole: кидает BadRequestException при невалидной роли")
  void updateUserRole_invalidRole() {
    User existing = new User();
    existing.setId(1L);
    existing.setRole(UserRole.REGULAR);
    when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> userService.updateUserRole(1L, "bad_role"))
        .isInstanceOf(BadRequestException.class);

    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("hasActiveSubscriptions: true если есть подписки")
  void hasActiveSubscriptions_true() {
    User existing = new User();
    existing.setId(2L);
    when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
    when(subscriptionRepository.findAllByUser(existing)).thenReturn(List.of(new ru.maxow.mvpn.subscription.Subscription()));

    boolean result = userService.hasActiveSubscriptions(2L);

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("getUserVerificationCode: кидает NotFoundException если нет пользователя")
  void getUserVerificationCode_notFound() {
    when(userRepository.findById(eq(404L))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getUserVerificationCode(404L))
        .isInstanceOf(NotFoundException.class);
  }
}
