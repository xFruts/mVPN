package ru.maxow.mvpn.user;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.maxow.mvpn.user.UserSpecifications.*;

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
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.domain.Specification;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;

@DataJpaTest
@DisplayName("UserSpecifications - Интеграционные тесты")
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
    Tariff basicTariff = new Tariff();
    basicTariff.setName("Basic");
    basicTariff.setMaxDevices(1);
    basicTariff.setTrafficLimitGb(10);
    basicTariff.setDurationOfDays(30);
    em.persist(basicTariff);

    Tariff premiumTariff = new Tariff();
    premiumTariff.setName("Premium");
    premiumTariff.setMaxDevices(5);
    premiumTariff.setTrafficLimitGb(100);
    premiumTariff.setDurationOfDays(30);
    em.persist(premiumTariff);

    alice = new User();
    alice.setFullName("Alice Johnson");
    alice.setRole(UserRole.REGULAR);
    em.persist(alice);

    bob = new User();
    bob.setFullName("Bob Smith");
    bob.setRole(UserRole.ADMIN);
    em.persist(bob);

    charlie = new User();
    charlie.setFullName("Charlie Brown");
    charlie.setRole(UserRole.SPECIAL);
    em.persist(charlie);

    // Alice → Basic, ACTIVE
    Subscription aliceSub = new Subscription();
    aliceSub.setUser(alice);
    aliceSub.setTariff(basicTariff);
    aliceSub.setStartDate(OffsetDateTime.now().minusMonths(1));
    aliceSub.setEndDate(OffsetDateTime.now().plusMonths(1));
    aliceSub.setStatus(SubscriptionStatus.ACTIVE);
    em.persist(aliceSub);

    // Bob → Premium, EXPIRED
    Subscription bobSub = new Subscription();
    bobSub.setUser(bob);
    bobSub.setTariff(premiumTariff);
    bobSub.setStartDate(OffsetDateTime.now().minusMonths(3));
    bobSub.setEndDate(OffsetDateTime.now().minusMonths(1));
    bobSub.setStatus(SubscriptionStatus.EXPIRED);
    em.persist(bobSub);

    // Charlie → Premium, ACTIVE + second subscription (Basic, CANCELED)
    Subscription charlieSub1 = new Subscription();
    charlieSub1.setUser(charlie);
    charlieSub1.setTariff(premiumTariff);
    charlieSub1.setStartDate(OffsetDateTime.now().minusMonths(2));
    charlieSub1.setEndDate(OffsetDateTime.now().plusMonths(2));
    charlieSub1.setStatus(SubscriptionStatus.ACTIVE);
    em.persist(charlieSub1);

    Subscription charlieSub2 = new Subscription();
    charlieSub2.setUser(charlie);
    charlieSub2.setTariff(basicTariff);
    charlieSub2.setStartDate(OffsetDateTime.now().minusMonths(6));
    charlieSub2.setEndDate(OffsetDateTime.now().minusMonths(3));
    charlieSub2.setStatus(SubscriptionStatus.CANCELED);
    em.persist(charlieSub2);

    em.flush();
  }

  @Nested
  @DisplayName("Фильтрация по роли (hasRole)")
  class HasRoleTests {

    @Test
    @DisplayName("Фильтр по REGULAR — возвращает только REGULAR пользователей")
    void filterByRegularRole() {
      Specification<User> spec = Specification.where(hasRole("REGULAR"));

      List<User> result = userRepository.findAll(spec);

      assertThat(result).extracting(User::getFullName).containsExactly("Alice Johnson");
    }

    @Test
    @DisplayName("Фильтр по ADMIN — возвращает только ADMIN")
    void filterByAdminRole() {
      Specification<User> spec = Specification.where(hasRole("ADMIN"));

      List<User> result = userRepository.findAll(spec);

      assertThat(result).extracting(User::getFullName).containsExactly("Bob Smith");
    }

    @Test
    @DisplayName("Пустой или null фильтр — возвращает всех пользователей")
    void nullOrBlankRoleReturnsAll() {
      assertThat(userRepository.findAll(Specification.where(hasRole(null))))
          .hasSize(3);
      assertThat(userRepository.findAll(Specification.where(hasRole(""))))
          .hasSize(3);
      assertThat(userRepository.findAll(Specification.where(hasRole("   "))))
          .hasSize(3);
    }

    @Test
    @DisplayName("Невалидная роль — возвращает пустой результат")
    void invalidRoleReturnsEmpty() {
      List<User> result = userRepository.findAll(Specification.where(hasRole("SUPERUSER")));

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Фильтрация по статусу подписки (hasSubscriptionStatus)")
  class HasSubscriptionStatusTests {

    @Test
    @DisplayName("Фильтр по ACTIVE — возвращает пользователей с активной подпиской")
    void filterByActiveStatus() {
      Specification<User> spec = Specification.where(hasSubscriptionStatus("ACTIVE"));

      List<User> result = userRepository.findAll(spec);

      assertThat(result).extracting(User::getFullName)
          .containsExactlyInAnyOrder("Alice Johnson", "Charlie Brown");
    }

    @Test
    @DisplayName("Фильтр по EXPIRED — возвращает пользователей с истёкшей подпиской")
    void filterByExpiredStatus() {
      List<User> result = userRepository.findAll(
          Specification.where(hasSubscriptionStatus("EXPIRED")));

      assertThat(result).extracting(User::getFullName).containsExactly("Bob Smith");
    }

    @Test
    @DisplayName("Фильтр по CANCELED — возвращает пользователей с отменённой подпиской")
    void filterByCanceledStatus() {
      List<User> result = userRepository.findAll(
          Specification.where(hasSubscriptionStatus("CANCELED")));

      assertThat(result).extracting(User::getFullName).containsExactly("Charlie Brown");
    }

    @Test
    @DisplayName("Пустой или null статус — возвращает всех пользователей")
    void nullOrBlankStatusReturnsAll() {
      assertThat(userRepository.findAll(Specification.where(hasSubscriptionStatus(null))))
          .hasSize(3);
      assertThat(userRepository.findAll(Specification.where(hasSubscriptionStatus(""))))
          .hasSize(3);
    }

    @Test
    @DisplayName("Пользователь с несколькими подписками не дублируется при фильтрации по статусу")
    void userWithMultipleSubscriptionsNotDuplicated() {
      List<User> result = userRepository.findAll(
          Specification.where(hasSubscriptionStatus("ACTIVE")));

      long charlieCount = result.stream()
          .filter(u -> u.getFullName().equals("Charlie Brown"))
          .count();
      assertThat(charlieCount).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("Фильтрация по имени (nameContains)")
  class NameContainsTests {

    @Test
    @DisplayName("Поиск по части имени — находит совпадения (регистронезависимо)")
    void searchByPartialName() {
      List<User> result = userRepository.findAll(Specification.where(nameContains("alice")));

      assertThat(result).extracting(User::getFullName).containsExactly("Alice Johnson");
    }

    @Test
    @DisplayName("Поиск по 'o' — находит всех пользователей с 'o' в имени")
    void searchBySingleChar() {
      List<User> result = userRepository.findAll(Specification.where(nameContains("o")));

      // "Bob Smith" (o in Bob), "Charlie Brown" (o in Brown), "Alice Johnson" (o in Johnson)
      assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("Поиск по несуществующему имени — возвращает пустой результат")
    void searchByNonExistentName() {
      List<User> result = userRepository.findAll(Specification.where(nameContains("zzzzz")));

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Пустой или null поиск — возвращает всех пользователей")
    void nullOrBlankSearchReturnsAll() {
      assertThat(userRepository.findAll(Specification.where(nameContains(null)))).hasSize(3);
      assertThat(userRepository.findAll(Specification.where(nameContains("")))).hasSize(3);
    }
  }

  @Nested
  @DisplayName("Комбинированные фильтры")
  class CombinedFilterTests {

    @Test
    @DisplayName("Роль + Статус — возвращает пользователей с обоими условиями")
    void roleAndStatus() {
      Specification<User> spec = Specification.where(hasRole("REGULAR"))
          .and(hasSubscriptionStatus("ACTIVE"));

      List<User> result = userRepository.findAll(spec);

      assertThat(result).extracting(User::getFullName).containsExactly("Alice Johnson");
    }

    @Test
    @DisplayName("Все фильтры вместе — возвращает только подходящего пользователя")
    void allFiltersCombined() {
      Specification<User> spec = Specification.where(hasRole("REGULAR"))
          .and(hasSubscriptionStatus("ACTIVE"))
          .and(nameContains("alice"));

      List<User> result = userRepository.findAll(spec);

      assertThat(result).extracting(User::getFullName).containsExactly("Alice Johnson");
    }

    @Test
    @DisplayName("Противоречивые фильтры — возвращает пустой результат")
    void conflictingFiltersReturnEmpty() {
      Specification<User> spec = Specification.where(hasRole("ADMIN"))
          .and(hasSubscriptionStatus("ACTIVE"));

      List<User> result = userRepository.findAll(spec);

      // Bob is ADMIN but has EXPIRED status
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Сортировка")
  class SortingTests {

    @Test
    @DisplayName("Сортировка по fullName ASC — в алфавитном порядке")
    void sortByFullNameAsc() {
      Page<User> page = userRepository.findAll(
          Specification.where(hasRole(null)),
          PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "fullName")));

      assertThat(page.getContent()).extracting(User::getFullName)
          .containsExactly("Alice Johnson", "Bob Smith", "Charlie Brown");
    }

    @Test
    @DisplayName("Сортировка по fullName DESC — в обратном порядке")
    void sortByFullNameDesc() {
      Page<User> page = userRepository.findAll(
          Specification.where(hasRole(null)),
          PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "fullName")));

      assertThat(page.getContent()).extracting(User::getFullName)
          .containsExactly("Charlie Brown", "Bob Smith", "Alice Johnson");
    }

    @Test
    @DisplayName("Сортировка по tariffName DESC — пользователи упорядочены по названию тарифа")
    void sortByTariffNameDesc() {
      Sort sort = JpaSort.unsafe(Sort.Direction.DESC, "subscriptions.tariff.name");

      Specification<User> spec = Specification.where(hasSubscriptionStatus("EXPIRED"));

      Page<User> page = userRepository.findAll(spec, PageRequest.of(0, 10, sort));

      // Only Bob has EXPIRED status (Premium tariff)
      assertThat(page.getContent())
          .hasSize(1)
          .extracting(User::getFullName)
          .containsExactly("Bob Smith");
    }

    @Test
    @DisplayName("Сортировка по subEndDate ASC — пользователи упорядочены по дате окончания подписки")
    void sortBySubEndDateAsc() {
      Sort sort = JpaSort.unsafe(Sort.Direction.ASC, "subscriptions.endDate");

      Specification<User> spec = Specification.where(hasSubscriptionStatus("ACTIVE"));

      Page<User> page = userRepository.findAll(spec, PageRequest.of(0, 10, sort));

      // Alice and Charlie have ACTIVE subscriptions
      assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Сортировка с фильтрацией + пагинация — корректный результат")
    void sortWithFilterAndPagination() {
      Sort sort = Sort.by(Sort.Direction.ASC, "fullName");

      Specification<User> spec = Specification.where(hasSubscriptionStatus("ACTIVE"));

      Page<User> page = userRepository.findAll(spec, PageRequest.of(0, 1, sort));

      assertThat(page.getContent()).hasSize(1);
      assertThat(page.getTotalElements()).isEqualTo(2);
      assertThat(page.getTotalPages()).isEqualTo(2);
      assertThat(page.getContent().getFirst().getFullName()).isEqualTo("Alice Johnson");
    }
  }
}
