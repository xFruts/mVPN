package ru.maxow.mvpn.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.maxow.mvpn.model.ListUserDto;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.tariff.TariffMapperImpl;

@DisplayName("UserMapper - mapping tests")
class UserMapperTest {

  private final UserMapperImpl userMapper = new UserMapperImpl();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(userMapper, "tariffMapper", new TariffMapperImpl());
  }

  @Test
  @DisplayName("Should map tariff name from latest subscription")
  void shouldMapTariffNameFromLatestSubscription() {
    User user = new User();
    user.setId(1L);
    user.setFullName("John Doe");
    user.setRole(UserRole.REGULAR);

    Tariff basicTariff = new Tariff();
    basicTariff.setId(10L);
    basicTariff.setName("Basic");

    Tariff premiumTariff = new Tariff();
    premiumTariff.setId(11L);
    premiumTariff.setName("Premium");

    Subscription oldSubscription = new Subscription();
    oldSubscription.setStartDate(OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    oldSubscription.setEndDate(OffsetDateTime.of(2025, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    oldSubscription.setStatus(SubscriptionStatus.ACTIVE);
    oldSubscription.setTariff(basicTariff);
    oldSubscription.setUser(user);

    Subscription latestSubscription = new Subscription();
    latestSubscription.setStartDate(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    latestSubscription.setEndDate(OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    latestSubscription.setStatus(SubscriptionStatus.CANCELED);
    latestSubscription.setTariff(premiumTariff);
    latestSubscription.setUser(user);

    user.getSubscriptions().addAll(List.of(oldSubscription, latestSubscription));

    ListUserDto result = userMapper.toListUserDto(user);

    assertThat(result.getTariffName()).isEqualTo("Premium");
    assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.CANCELED);
    assertThat(result.getEndDate()).isEqualTo(latestSubscription.getEndDate());
  }

  @Test
  @DisplayName("Should return null tariff name when user has no subscriptions")
  void shouldReturnNullTariffNameWhenUserHasNoSubscriptions() {
    User user = new User();
    user.setId(1L);
    user.setFullName("John Doe");
    user.setRole(UserRole.REGULAR);

    ListUserDto result = userMapper.toListUserDto(user);

    assertThat(result.getTariffName()).isNull();
    assertThat(result.getSubscriptionStatus()).isNull();
    assertThat(result.getEndDate()).isNull();
  }

  @Test
  @DisplayName("Should return null tariff name when latest subscription has no tariff")
  void shouldReturnNullTariffNameWhenLatestSubscriptionHasNoTariff() {
    User user = new User();
    user.setId(1L);
    user.setFullName("John Doe");
    user.setRole(UserRole.REGULAR);

    Subscription subscription = new Subscription();
    subscription.setStartDate(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    subscription.setEndDate(OffsetDateTime.of(2026, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setTariff(null);
    subscription.setUser(user);

    user.getSubscriptions().add(subscription);

    ListUserDto result = userMapper.toListUserDto(user);

    assertThat(result.getTariffName()).isNull();
    assertThat(result.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(result.getEndDate()).isEqualTo(subscription.getEndDate());
  }
}

