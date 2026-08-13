package ru.maxow.mvpn.user;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.maxow.mvpn.user.UserSpecifications.hasRole;
import static ru.maxow.mvpn.user.UserSpecifications.hasSubscriptionStatus;
import static ru.maxow.mvpn.user.UserSpecifications.nameContains;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;

@DataJpaTest
@DisplayName("UserSpecifications")
class UserSpecificationsTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private TestEntityManager em;

  private User alice;
  private User bob;
  private User charlie;

  @BeforeEach
  void setUp() {
    Tariff basic = persistTariff("Basic", 1, 10);
    Tariff premium = persistTariff("Premium", 5, 100);

    alice = persistUser("Alice Johnson", UserRole.REGULAR);
    bob = persistUser("Bob Smith", UserRole.ADMIN);
    charlie = persistUser("Charlie Brown", UserRole.SPECIAL);

    persistSubscription(alice, basic, OffsetDateTime.now().minusMonths(1),
        OffsetDateTime.now().plusMonths(1), SubscriptionStatus.ACTIVE);
    persistSubscription(bob, premium, OffsetDateTime.now().minusMonths(3),
        OffsetDateTime.now().minusMonths(1), SubscriptionStatus.EXPIRED);
    persistSubscription(charlie, premium, OffsetDateTime.now().minusMonths(2),
        OffsetDateTime.now().plusMonths(2), SubscriptionStatus.ACTIVE);
    persistSubscription(charlie, basic, OffsetDateTime.now().minusMonths(6),
        OffsetDateTime.now().minusMonths(3), SubscriptionStatus.CANCELED);

    em.flush();
  }

  @Nested
  @DisplayName("hasRole")
  class HasRole {

    @Test
    @DisplayName("filters by valid role")
    void validRole() {
      assertThat(userRepository.findAll(Specification.where(hasRole("REGULAR"))))
          .extracting(User::getFullName)
          .containsExactly("Alice Johnson");
    }

    @Test
    @DisplayName("treats null/blank as no filter")
    void nullOrBlank() {
      assertThat(userRepository.findAll(Specification.where(hasRole(null)))).hasSize(3);
      assertThat(userRepository.findAll(Specification.where(hasRole("")))).hasSize(3);
      assertThat(userRepository.findAll(Specification.where(hasRole("   ")))).hasSize(3);
    }

    @Test
    @DisplayName("returns empty for unknown role")
    void invalidRole() {
      assertThat(userRepository.findAll(Specification.where(hasRole("SUPERUSER")))).isEmpty();
    }
  }

  @Nested
  @DisplayName("hasSubscriptionStatus")
  class HasSubscriptionStatus {

    @Test
    @DisplayName("filters users that have a subscription with given status")
    void active() {
      assertThat(userRepository.findAll(Specification.where(hasSubscriptionStatus("ACTIVE"))))
          .extracting(User::getFullName)
          .containsExactlyInAnyOrder("Alice Johnson", "Charlie Brown");
    }

    @Test
    @DisplayName("does not duplicate users with multiple matching subscriptions")
    void noDuplicates() {
      long charlieCount = userRepository.findAll(Specification.where(hasSubscriptionStatus("ACTIVE")))
          .stream()
          .filter(u -> u.getFullName().equals("Charlie Brown"))
          .count();

      assertThat(charlieCount).isEqualTo(1);
    }

    @Test
    @DisplayName("returns empty for unknown status")
    void invalidStatus() {
      assertThat(userRepository.findAll(Specification.where(hasSubscriptionStatus("UNKNOWN"))))
          .isEmpty();
    }

    @Test
    @DisplayName("treats null/blank as no filter")
    void nullOrBlank() {
      assertThat(userRepository.findAll(Specification.where(hasSubscriptionStatus(null)))).hasSize(3);
      assertThat(userRepository.findAll(Specification.where(hasSubscriptionStatus("")))).hasSize(3);
    }
  }

  @Nested
  @DisplayName("nameContains")
  class NameContains {

    @Test
    @DisplayName("matches case-insensitively by substring")
    void caseInsensitive() {
      assertThat(userRepository.findAll(Specification.where(nameContains("alice"))))
          .extracting(User::getFullName)
          .containsExactly("Alice Johnson");
    }

    @Test
    @DisplayName("treats LIKE wildcards as literals")
    void escapesWildcards() {
      User weird = persistUser("100%_ready", UserRole.REGULAR);
      em.flush();

      assertThat(userRepository.findAll(Specification.where(nameContains("%_"))))
          .extracting(User::getFullName)
          .containsExactly(weird.getFullName());
      assertThat(userRepository.findAll(Specification.where(nameContains("%"))))
          .extracting(User::getFullName)
          .contains(weird.getFullName())
          .doesNotContain("Alice Johnson", "Bob Smith", "Charlie Brown");
    }

    @Test
    @DisplayName("returns empty when nothing matches")
    void noMatch() {
      assertThat(userRepository.findAll(Specification.where(nameContains("zzzzz")))).isEmpty();
    }

    @Test
    @DisplayName("treats null/blank as no filter")
    void nullOrBlank() {
      assertThat(userRepository.findAll(Specification.where(nameContains(null)))).hasSize(3);
      assertThat(userRepository.findAll(Specification.where(nameContains("")))).hasSize(3);
    }
  }

  @Nested
  @DisplayName("combined filters and paging")
  class Combined {

    @Test
    @DisplayName("applies role + status + search together")
    void allFilters() {
      Specification<User> spec = Specification.where(hasRole("REGULAR"))
          .and(hasSubscriptionStatus("ACTIVE"))
          .and(nameContains("alice"));

      assertThat(userRepository.findAll(spec))
          .extracting(User::getFullName)
          .containsExactly("Alice Johnson");
    }

    @Test
    @DisplayName("returns empty for conflicting filters")
    void conflicting() {
      Specification<User> spec = Specification.where(hasRole("ADMIN"))
          .and(hasSubscriptionStatus("ACTIVE"));

      assertThat(userRepository.findAll(spec)).isEmpty();
    }

    @Test
    @DisplayName("supports sort and pagination on filtered result")
    void sortAndPage() {
      Page<User> page = userRepository.findAll(
          Specification.where(hasSubscriptionStatus("ACTIVE")),
          PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "fullName")));

      assertThat(page.getTotalElements()).isEqualTo(2);
      assertThat(page.getTotalPages()).isEqualTo(2);
      assertThat(page.getContent())
          .extracting(User::getFullName)
          .containsExactly("Alice Johnson");
    }
  }

  @Nested
  @DisplayName("escapeLikePattern")
  class EscapeLikePattern {

    @Test
    @DisplayName("escapes backslash, percent and underscore")
    void escapes() {
      assertThat(UserSpecifications.escapeLikePattern("a%b_c\\d"))
          .isEqualTo("a\\%b\\_c\\\\d");
    }
  }

  private Tariff persistTariff(String name, int devices, int trafficGb) {
    Tariff tariff = new Tariff();
    tariff.setName(name);
    tariff.setMaxDevices(devices);
    tariff.setTrafficLimitGb(trafficGb);
    tariff.setDurationOfDays(30);
    return em.persist(tariff);
  }

  private User persistUser(String fullName, UserRole role) {
    User user = new User();
    user.setFullName(fullName);
    user.setRole(role);
    return em.persist(user);
  }

  private void persistSubscription(
      User user,
      Tariff tariff,
      OffsetDateTime start,
      OffsetDateTime end,
      SubscriptionStatus status) {
    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setTariff(tariff);
    subscription.setStartDate(start);
    subscription.setEndDate(end);
    subscription.setStatus(status);
    em.persist(subscription);
  }
}
