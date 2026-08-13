package ru.maxow.mvpn.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
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

@DisplayName("UserMapper")
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
    @DisplayName("maps identity fields and latest subscription by startDate")
    void mapsLatestSubscription() {
      User user = user(1L, "John Doe", UserRole.REGULAR);
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
      assertThat(result.getTariffName()).isEqualTo("Premium");
      assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
      assertThat(result.getEndDate()).isEqualTo(OffsetDateTime.parse("2026-02-01T00:00:00Z"));
    }

    @Test
    @DisplayName("returns null subscription fields when user has no subscriptions")
    void noSubscriptions() {
      ListUserDto result = userMapper.toListUserDto(user(1L, "John Doe", UserRole.REGULAR));

      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getFullName()).isEqualTo("John Doe");
      assertThat(result.getRole()).isEqualTo("REGULAR");
      assertThat(result.getTariffName()).isNull();
      assertThat(result.getSubscriptionStatus()).isNull();
      assertThat(result.getEndDate()).isNull();
    }

    @Test
    @DisplayName("keeps subscription metadata when latest tariff is missing")
    void latestSubscriptionWithoutTariff() {
      User user = user(1L, "John Doe", UserRole.REGULAR);
      Subscription subscription = subscription(
          user, "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z",
          SubscriptionStatus.ACTIVE, null);
      user.getSubscriptions().add(subscription);

      ListUserDto result = userMapper.toListUserDto(user);

      assertThat(result.getTariffName()).isNull();
      assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
      assertThat(result.getEndDate()).isEqualTo(subscription.getEndDate());
    }

    @Test
    @DisplayName("skips subscriptions with null startDate when selecting latest")
    void skipsNullStartDate() {
      User user = user(1L, "John Doe", UserRole.REGULAR);

      Subscription withoutStart = subscription(
          user, null, "2024-01-01T00:00:00Z",
          SubscriptionStatus.EXPIRED, tariff(10L, "Legacy"));
      Subscription withStart = subscription(
          user, "2026-01-01T00:00:00Z", "2026-02-01T00:00:00Z",
          SubscriptionStatus.ACTIVE, tariff(11L, "Current"));
      user.getSubscriptions().addAll(List.of(withoutStart, withStart));

      ListUserDto result = userMapper.toListUserDto(user);

      assertThat(result.getTariffName()).isEqualTo("Current");
      assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("returns no subscription fields when all startDates are null")
    void allStartDatesNull() {
      User user = user(1L, "John Doe", UserRole.REGULAR);
      user.getSubscriptions().add(subscription(
          user, null, "2024-01-01T00:00:00Z",
          SubscriptionStatus.EXPIRED, tariff(10L, "Legacy")));

      ListUserDto result = userMapper.toListUserDto(user);

      assertThat(result.getTariffName()).isNull();
      assertThat(result.getSubscriptionStatus()).isNull();
      assertThat(result.getEndDate()).isNull();
    }

    @Test
    @DisplayName("returns null for null input")
    void nullUser() {
      assertThat(userMapper.toListUserDto(null)).isNull();
    }
  }

  @Nested
  @DisplayName("toShortListUserDto")
  class ToShortListUserDto {

    @Test
    @DisplayName("exposes only id and fullName")
    void mapsMinimalProjection() {
      User user = user(42L, "Jane Roe", UserRole.ADMIN);
      user.setUserTelegramId(999L);
      user.setVerificationCode(UUID.randomUUID());

      ShortListUserDto result = userMapper.toShortListUserDto(user);

      assertThat(result.getId()).isEqualTo(42L);
      assertThat(result.getFullName()).isEqualTo("Jane Roe");
    }
  }

  @Nested
  @DisplayName("toUser")
  class ToUser {

    @Test
    @DisplayName("maps request fields and does not accept client-controlled secrets")
    void ignoresSensitiveGeneratedFields() {
      CreateUserRequestDto request = new CreateUserRequestDto()
          .fullName("New User")
          .role(UserRole.SPECIAL)
          .userTelegramId(111L);

      User result = userMapper.toUser(request);

      assertThat(result.getId()).isNull();
      assertThat(result.getFullName()).isEqualTo("New User");
      assertThat(result.getRole()).isEqualTo(UserRole.SPECIAL);
      assertThat(result.getUserTelegramId()).isEqualTo(111L);
      assertThat(result.getVerificationCode()).isNotNull();
      assertThat(result.getXuiId()).isNotNull();
      assertThat(result.getXuiSubscription()).isNotNull();
      assertThat(result.getSubscriptions()).isEmpty();
    }
  }

  @Nested
  @DisplayName("toUserResponseDto")
  class ToUserResponseDto {

    @Test
    @DisplayName("maps user and attaches latest subscription")
    void mapsWithLatestSubscription() {
      User user = user(7L, "Alice", UserRole.REGULAR);
      user.setUserTelegramId(555L);
      user.getSubscriptions().add(
          subscription(user, "2026-03-01T00:00:00Z", "2026-04-01T00:00:00Z",
              SubscriptionStatus.ACTIVE, tariff(5L, "Pro")));

      UserResponseDto result = userMapper.toUserResponseDto(user);

      assertThat(result.getId()).isEqualTo(7L);
      assertThat(result.getFullName()).isEqualTo("Alice");
      assertThat(result.getRole()).isEqualTo(UserRole.REGULAR);
      assertThat(result.getUserTelegramId()).isEqualTo(555L);
      assertThat(result.getSubscription()).isNotNull();
      assertThat(result.getSubscription().getUserId()).isEqualTo(7L);
      assertThat(result.getSubscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("omits subscription when user has none")
    void mapsWithoutSubscription() {
      UserResponseDto result = userMapper.toUserResponseDto(user(7L, "Alice", UserRole.REGULAR));

      assertThat(result.getSubscription()).isNull();
    }
  }

  private static User user(Long id, String fullName, UserRole role) {
    User user = new User();
    user.setId(id);
    user.setFullName(fullName);
    user.setRole(role);
    return user;
  }

  private static Tariff tariff(Long id, String name) {
    Tariff tariff = new Tariff();
    tariff.setId(id);
    tariff.setName(name);
    return tariff;
  }

  private static Subscription subscription(
      User user,
      String start,
      String end,
      SubscriptionStatus status,
      Tariff tariff) {
    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setStartDate(start == null ? null : OffsetDateTime.parse(start).withOffsetSameInstant(ZoneOffset.UTC));
    subscription.setEndDate(OffsetDateTime.parse(end).withOffsetSameInstant(ZoneOffset.UTC));
    subscription.setStatus(status);
    subscription.setTariff(tariff);
    return subscription;
  }
}
