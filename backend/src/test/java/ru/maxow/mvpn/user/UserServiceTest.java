package ru.maxow.mvpn.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.ListUserDto;
import ru.maxow.mvpn.model.PageListUserDto;
import ru.maxow.mvpn.model.ShortListUserDto;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.model.UpdateUserRequestDto;
import ru.maxow.mvpn.model.UserResponseDto;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.ResourceAlreadyExistsException;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
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
  private UUID verificationCode;

  @BeforeEach
  void setUp() {
    verificationCode = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    testUser = new User();
    testUser.setId(1L);
    testUser.setFullName("John Doe");
    testUser.setRole(UserRole.REGULAR);
    testUser.setVerificationCode(verificationCode);
    testUser.setUserTelegramId(123456789L);
  }

  @Nested
  @DisplayName("createUser")
  class CreateUser {

    @Test
    @DisplayName("persists mapped user when fullName is unique")
    void createsWhenUnique() {
      CreateUserRequestDto request = new CreateUserRequestDto().fullName("New User");
      User mapped = new User();
      mapped.setId(2L);
      mapped.setFullName("New User");
      mapped.setRole(UserRole.REGULAR);
      UserResponseDto response = new UserResponseDto().id(2L).fullName("New User").role(UserRole.REGULAR);

      when(userRepository.existsByFullName("New User")).thenReturn(false);
      when(userMapper.toUser(request)).thenReturn(mapped);
      when(userRepository.save(mapped)).thenReturn(mapped);
      when(userMapper.toUserResponseDto(mapped)).thenReturn(response);

      UserResponseDto result = userService.createUser(request);

      assertThat(result.getId()).isEqualTo(2L);
      assertThat(result.getFullName()).isEqualTo("New User");
      verify(userRepository).save(mapped);
    }

    @Test
    @DisplayName("rejects duplicate fullName before mapping or persist")
    void rejectsDuplicateFullName() {
      CreateUserRequestDto request = new CreateUserRequestDto().fullName("New User");
      when(userRepository.existsByFullName("New User")).thenReturn(true);

      assertThatThrownBy(() -> userService.createUser(request))
          .isInstanceOf(ResourceAlreadyExistsException.class)
          .hasMessageContaining("already exists");

      verify(userMapper, never()).toUser(any());
      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("returns mapped dto for existing user")
    void found() {
      UserResponseDto response = new UserResponseDto()
          .id(1L)
          .fullName("John Doe")
          .role(UserRole.REGULAR);
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(userMapper.toUserResponseDto(testUser)).thenReturn(response);

      assertThat(userService.findById(1L)).isSameAs(response);
    }

    @Test
    @DisplayName("throws NotFoundException when user is missing")
    void missing() {
      when(userRepository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.findById(404L))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("User")
          .hasMessageContaining("404");

      verifyNoInteractions(userMapper);
    }
  }

  @Nested
  @DisplayName("updateUser")
  class UpdateUser {

    @Test
    @DisplayName("updates fullName when unique")
    void updatesFullName() {
      UpdateUserRequestDto dto = new UpdateUserRequestDto().fullName("Updated Name");
      UserResponseDto response = new UserResponseDto().id(1L).fullName("Updated Name");

      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(userRepository.existsByFullName("Updated Name")).thenReturn(false);
      when(userRepository.save(testUser)).thenReturn(testUser);
      when(userMapper.toUserResponseDto(testUser)).thenReturn(response);

      UserResponseDto result = userService.updateUser(1L, dto);

      assertThat(result.getFullName()).isEqualTo("Updated Name");
      assertThat(testUser.getFullName()).isEqualTo("Updated Name");
      verify(subscriptionRepository, never()).findFirstByUserOrderByStartDateDesc(any());
    }

    @Test
    @DisplayName("rejects fullName that belongs to another user")
    void rejectsTakenFullName() {
      UpdateUserRequestDto dto = new UpdateUserRequestDto().fullName("Taken Name");
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(userRepository.existsByFullName("Taken Name")).thenReturn(true);

      assertThatThrownBy(() -> userService.updateUser(1L, dto))
          .isInstanceOf(ResourceAlreadyExistsException.class);

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("allows keeping the same fullName without uniqueness conflict")
    void allowsSameFullName() {
      UpdateUserRequestDto dto = new UpdateUserRequestDto().fullName("John Doe");
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(userRepository.save(testUser)).thenReturn(testUser);
      when(userMapper.toUserResponseDto(testUser)).thenReturn(new UserResponseDto().id(1L));

      userService.updateUser(1L, dto);

      verify(userRepository, never()).existsByFullName(any());
      verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updates latest subscription only when subscription fields are present")
    void updatesLatestSubscription() {
      OffsetDateTime endDate = OffsetDateTime.parse("2026-12-31T00:00:00Z");
      UpdateUserRequestDto dto = new UpdateUserRequestDto()
          .subscriptionStatus(SubscriptionStatus.CANCELED)
          .subscriptionEndDate(endDate);
      Subscription latest = new Subscription();
      latest.setStatus(SubscriptionStatus.ACTIVE);

      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(subscriptionRepository.findFirstByUserOrderByStartDateDesc(testUser))
          .thenReturn(Optional.of(latest));
      when(userRepository.save(testUser)).thenReturn(testUser);
      when(userMapper.toUserResponseDto(testUser)).thenReturn(new UserResponseDto().id(1L));

      userService.updateUser(1L, dto);

      assertThat(latest.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
      assertThat(latest.getEndDate()).isEqualTo(endDate);
      verify(subscriptionRepository).save(latest);
    }

    @Test
    @DisplayName("throws NotFoundException for missing user")
    void missingUser() {
      when(userRepository.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> userService.updateUser(999L, new UpdateUserRequestDto().fullName("X")))
          .isInstanceOf(NotFoundException.class);

      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("deleteUserById")
  class DeleteUser {

    @Test
    @DisplayName("deletes when user exists")
    void deletesExisting() {
      when(userRepository.existsById(1L)).thenReturn(true);

      userService.deleteUserById(1L);

      verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("throws NotFoundException when user does not exist")
    void missing() {
      when(userRepository.existsById(404L)).thenReturn(false);

      assertThatThrownBy(() -> userService.deleteUserById(404L))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("404");

      verify(userRepository, never()).deleteById(any());
    }
  }

  @Nested
  @DisplayName("updateUserRole")
  class UpdateUserRole {

    @Test
    @DisplayName("updates role using case-insensitive enum value")
    void updatesRole() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(userRepository.save(testUser)).thenReturn(testUser);
      when(userMapper.toUserResponseDto(testUser))
          .thenReturn(new UserResponseDto().id(1L).role(UserRole.ADMIN));

      UserResponseDto result = userService.updateUserRole(1L, "admin");

      assertThat(testUser.getRole()).isEqualTo(UserRole.ADMIN);
      assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("rejects invalid role without persisting")
    void invalidRole() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

      assertThatThrownBy(() -> userService.updateUserRole(1L, "INVALID_ROLE"))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Invalid user role");

      verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("rejects blank role")
    void blankRole() {
      assertThatThrownBy(() -> userService.updateUserRole(1L, "  "))
          .isInstanceOf(BadRequestException.class);

      verify(userRepository, never()).findById(any());
      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("verification code")
  class VerificationCode {

    @Test
    @DisplayName("checkVerificationCode delegates to repository")
    void checkDelegates() {
      when(userRepository.existsByVerificationCode(verificationCode)).thenReturn(true);

      assertThat(userService.checkVerificationCode(verificationCode)).isTrue();
    }

    @Test
    @DisplayName("getUserVerificationCode returns stored UUID")
    void getCode() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

      assertThat(userService.getUserVerificationCode(1L)).isEqualTo(verificationCode);
    }

    @Test
    @DisplayName("updateUserTelegramId binds telegram id when code matches")
    void bindTelegramId() {
      when(userRepository.findByVerificationCode(verificationCode)).thenReturn(Optional.of(testUser));

      userService.updateUserTelegramId(verificationCode, 777L);

      assertThat(testUser.getUserTelegramId()).isEqualTo(777L);
      verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("updateUserTelegramId is a no-op for unknown code (anti-enumeration)")
    void unknownCodeNoOp() {
      UUID unknown = UUID.randomUUID();
      when(userRepository.findByVerificationCode(unknown)).thenReturn(Optional.empty());

      userService.updateUserTelegramId(unknown, 777L);

      verify(userRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("subscriptions presence")
  class HasAnySubscriptions {

    @Test
    @DisplayName("returns false when user has no subscriptions")
    void none() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(subscriptionRepository.findFirstByUserOrderByStartDateDesc(testUser))
          .thenReturn(Optional.empty());

      assertThat(userService.hasAnySubscriptions(1L)).isFalse();
    }

    @Test
    @DisplayName("returns true when at least one subscription exists")
    void present() {
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(subscriptionRepository.findFirstByUserOrderByStartDateDesc(testUser))
          .thenReturn(Optional.of(new Subscription()));

      assertThat(userService.hasAnySubscriptions(1L)).isTrue();
    }
  }

  @Nested
  @DisplayName("telegram lookups")
  class TelegramLookups {

    @Test
    @DisplayName("findByTelegramId returns user or null")
    void findByTelegramId() {
      when(userRepository.findByUserTelegramId(123456789L)).thenReturn(Optional.of(testUser));
      when(userRepository.findByUserTelegramId(1L)).thenReturn(Optional.empty());

      assertThat(userService.findByTelegramId(123456789L)).isSameAs(testUser);
      assertThat(userService.findByTelegramId(1L)).isNull();
    }

    @Test
    @DisplayName("getUsersByTelegramIds returns repository result")
    void byTelegramIds() {
      List<Long> ids = List.of(111L, 222L);
      when(userRepository.findByUserTelegramIdIn(ids)).thenReturn(List.of(testUser));

      assertThat(userService.getUsersByTelegramIds(ids)).containsExactly(testUser);
    }

    @Test
    @DisplayName("getUsersByRole returns repository result")
    void byRole() {
      when(userRepository.findAllByRole(UserRole.REGULAR)).thenReturn(List.of(testUser));

      assertThat(userService.getUsersByRole(UserRole.REGULAR)).containsExactly(testUser);
    }
  }

  @Nested
  @DisplayName("findAllAsList")
  class FindAllAsList {

    @Test
    @DisplayName("maps users to ShortListUserDto")
    void mapsShortList() {
      ShortListUserDto dto = new ShortListUserDto().id(1L).fullName("John Doe");
      when(userRepository.findAll()).thenReturn(List.of(testUser));
      when(userMapper.toShortListUserDto(testUser)).thenReturn(dto);

      List<ShortListUserDto> result = userService.findAllAsList();

      assertThat(result).containsExactly(dto);
      verify(userMapper).toShortListUserDto(testUser);
      verify(userMapper, never()).toListUserDto(any());
    }

    @Test
    @DisplayName("returns empty list when repository is empty")
    void empty() {
      when(userRepository.findAll()).thenReturn(List.of());

      assertThat(userService.findAllAsList()).isEmpty();
      verifyNoInteractions(userMapper);
    }
  }

  @Nested
  @DisplayName("findAllAsPage")
  class FindAllAsPage {

    @SuppressWarnings("unchecked")
    private void stubPage() {
      when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1));
      when(userMapper.toListUserDto(testUser)).thenReturn(new ListUserDto().id(1L));
    }

    @Test
    @DisplayName("uses unsorted pageable when sort is null")
    void unsortedWhenSortNull() {
      stubPage();

      PageListUserDto page = userService.findAllAsPage(0, 10, null, null, null, null);

      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      verify(userRepository).findAll(any(Specification.class), captor.capture());
      assertThat(captor.getValue().getSort().isUnsorted()).isTrue();
      assertThat(page.getContent()).hasSize(1);
      assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("parses comma-separated sort pairs")
    void commaSeparatedSort() {
      stubPage();

      userService.findAllAsPage(0, 10, List.of("fullName,desc", "role,invalid"), null, null, null);

      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      verify(userRepository).findAll(any(Specification.class), captor.capture());
      Sort sort = captor.getValue().getSort();
      assertThat(sort.getOrderFor("fullName").isDescending()).isTrue();
      assertThat(sort.getOrderFor("role").isAscending()).isTrue();
    }

    @Test
    @DisplayName("relinks field/direction when Spring splits sort query params")
    void springSplitSort() {
      stubPage();

      userService.findAllAsPage(0, 10, List.of("id", "desc"), null, null, null);

      ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
      verify(userRepository).findAll(any(Specification.class), captor.capture());
      Sort sort = captor.getValue().getSort();
      assertThat(sort.toList()).hasSize(1);
      assertThat(sort.getOrderFor("id").isDescending()).isTrue();
    }
  }
}
