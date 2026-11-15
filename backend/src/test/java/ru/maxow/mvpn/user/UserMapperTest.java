package ru.maxow.mvpn.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.maxow.mvpn.model.CreateUserRequestDto;
import ru.maxow.mvpn.model.ListUserDto;
import ru.maxow.mvpn.model.ShortListUserDto;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.model.UserResponseDto;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.tariff.TariffMapperImpl;

@DisplayName("UserMapper - Unit Tests")
class UserMapperTest {

  private final UserMapperImpl userMapper = new UserMapperImpl();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(userMapper, "tariffMapper", new TariffMapperImpl());
  }

  @Nested
  @DisplayName("toListUserDto")
  class ToListUserDto {

    @Test
    @DisplayName("maps identity fields, telegramUsername, and latest subscription")
    void mapsLatestSubscription() {
      User user = user(1L, "John Doe", UserRole.REGULAR);
      user.setTelegramUsername("xFruts");
      user.getSubscriptions().addAll(List.of(
          subscription(user, "2025-01-01T00:00:00Z", "2025-02-01T00:00:00Z",
              SubscriptionStatus.ACTIVE, tariff(10L, "Basic")),
          subscription(user, "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z",
              SubscriptionStatus.CANCELED, tariff(11L, "Premium"))
      ));

      ListUserDto result = userMapper.toListUserDto(user);

      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getFullName()).isEqualTo("John Doe");
      assertThat(result.getRole()).isEqualTo("REGULAR");
      assertThat(result.getTelegramUsername()).isEqualTo("xfruts");
      assertThat(result.getTariffName()).isEqualTo("Premium");
      assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
      assertThat(result.getEndDate()).isEqualTo(OffsetDateTime.parse("2026-02-01T00:00:00Z"));
    }

    @Test
    @DisplayName("handles user with no subscriptions gracefully")
    void noSubscriptions() {
      User user = user(2L, "Jane Doe", UserRole.ADMIN);

      ListUserDto result = userMapper.toListUserDto(user);

      assertThat(result.getId()).isEqualTo(2L);
      assertThat(result.getFullName()).isEqualTo("Jane Doe");
      assertThat(result.getRole()).isEqualTo("ADMIN");
      assertThat(result.getTelegramUsername()).isNull();
      assertThat(result.getTariffName()).isNull();
      assertThat(result.getSubscriptionStatus()).isNull();
      assertThat(result.getEndDate()).isNull();
    }

    @Test
    @DisplayName("handles latest subscription without tariff")
    void latestSubscriptionWithoutTariff() {
      User user = user(3L, "Bob", UserRole.SPECIAL);
      user.getSubscriptions().add(
          subscription(user, "2026-03-01T00:00:00Z", "2026-04-01T00:00:00Z",
              SubscriptionStatus.ACTIVE, null)
      );

      ListUserDto result = userMapper.toListUserDto(user);

      assertThat(result.getTariffName()).isNull();
      assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }
  }

  @Nested
  @DisplayName("toUserResponseDto")
  class ToUserResponseDto {

    @Test
    @DisplayName("maps identity and latest subscription via enrichUserResponseDto")
    void mapsIdentityAndLatestSubscription() {
      User user = user(1L, "John Doe", UserRole.REGULAR);
      user.setTelegramUsername("john_tg");
      user.setUserTelegramId(999L);
      user.getSubscriptions().add(
          subscription(user, "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z",
              SubscriptionStatus.ACTIVE, tariff(5L, "Pro"))
      );

      UserResponseDto result = userMapper.toUserResponseDto(user);

      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getFullName()).isEqualTo("John Doe");
      assertThat(result.getRole()).isEqualTo(UserRole.REGULAR);
      assertThat(result.getTelegramUsername()).isEqualTo("john_tg");
      assertThat(result.getUserTelegramId()).isEqualTo(999L);
      assertThat(result.getSubscription()).isNotNull();
      assertThat(result.getSubscription().getUserId()).isEqualTo(1L);
      assertThat(result.getSubscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      assertThat(result.getSubscription().getTariff()).isNotNull();
      assertThat(result.getSubscription().getTariff().getName()).isEqualTo("Pro");
    }

    @Test
    @DisplayName("subscription is null when user has no subscriptions")
    void emptySubscriptionsYieldsNullSubscriptionDto() {
      User user = user(2L, "Jane Doe", UserRole.ADMIN);

      UserResponseDto result = userMapper.toUserResponseDto(user);

      assertThat(result.getSubscription()).isNull();
    }
  }

  @Nested
  @DisplayName("toShortListUserDto")
  class ToShortListUserDto {

    @Test
    @DisplayName("maps id and fullName")
    void mapsBasicFields() {
      User user = user(42L, "Alice", UserRole.REGULAR);

      ShortListUserDto result = userMapper.toShortListUserDto(user);

      assertThat(result.getId()).isEqualTo(42L);
      assertThat(result.getFullName()).isEqualTo("Alice");
    }
  }

  @Nested
  @DisplayName("toUser from CreateUserRequestDto")
  class ToUserFromCreateRequest {

    @Test
    @DisplayName("maps fullName and role, ignores id and autogenerated fields")
    void mapsCreateRequest() {
      CreateUserRequestDto request = new CreateUserRequestDto()
          .fullName("New User")
          .role(UserRole.SPECIAL)
          .telegramUsername("@newbie")
          .userTelegramId(12345L);

      User user = userMapper.toUser(request);

      assertThat(user.getId()).isNull();
      assertThat(user.getFullName()).isEqualTo("New User");
      assertThat(user.getRole()).isEqualTo(UserRole.SPECIAL);
      assertThat(user.getTelegramUsername()).isEqualTo("newbie");
      assertThat(user.getUserTelegramId()).isEqualTo(12345L);
      assertThat(user.getVerificationCode()).isNotNull();
      assertThat(user.getXuiId()).isNotNull();
      assertThat(user.getXuiSubscription()).isNotNull();
      assertThat(user.isDeleted()).isFalse();
    }
  }

  private User user(Long id, String fullName, UserRole role) {
    User user = new User();
    user.setId(id);
    user.setFullName(fullName);
    user.setRole(role);
    return user;
  }

  private Subscription subscription(User user, String startIso, String endIso,
                                    SubscriptionStatus status, Tariff tariff) {
    Subscription s = new Subscription();
    s.setUser(user);
    s.setStartDate(OffsetDateTime.parse(startIso));
    s.setEndDate(OffsetDateTime.parse(endIso));
    s.setStatus(status);
    s.setTariff(tariff);
    return s;
  }

  private Tariff tariff(Long id, String name) {
    Tariff t = new Tariff();
    t.setId(id);
    t.setName(name);
    return t;
  }
}
