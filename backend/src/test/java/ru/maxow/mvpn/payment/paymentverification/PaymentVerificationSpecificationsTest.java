package ru.maxow.mvpn.payment.paymentverification;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.maxow.mvpn.payment.paymentverification.PaymentVerificationSpecifications.createdAtFrom;
import static ru.maxow.mvpn.payment.paymentverification.PaymentVerificationSpecifications.createdAtTo;
import static ru.maxow.mvpn.payment.paymentverification.PaymentVerificationSpecifications.fullNameContains;
import static ru.maxow.mvpn.payment.paymentverification.PaymentVerificationSpecifications.hasStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.model.VerificationStatus;
import ru.maxow.mvpn.user.User;

@DataJpaTest
@DisplayName("PaymentVerificationSpecifications - Интеграционные тесты")
class PaymentVerificationSpecificationsTest {

  @Autowired
  private PaymentVerificationRepository repository;

  @Autowired
  private TestEntityManager em;

  private PaymentVerification pendingIvan;
  private PaymentVerification approvedPetr;
  private PaymentVerification rejectedAnna;

  @BeforeEach
  void setUp() {
    User ivan = persistUser("Ivan Petrov");
    User petr = persistUser("Petr Sidorov");
    User anna = persistUser("Anna Smirnova");

    Instant now = Instant.now();

    pendingIvan = persistVerification(
        ivan,
        "Ivanov I.I.",
        VerificationStatus.PENDING,
        now.minus(2, ChronoUnit.DAYS),
        "2026-09-01");

    approvedPetr = persistVerification(
        petr,
        "Sidorov P.P.",
        VerificationStatus.APPROVED,
        now.minus(1, ChronoUnit.DAYS),
        "2026-09-15");

    rejectedAnna = persistVerification(
        anna,
        "Smirnova A.A.",
        VerificationStatus.REJECTED,
        now.minus(5, ChronoUnit.HOURS),
        "2026-10-01");

    em.flush();
    em.clear();
  }

  @Nested
  @DisplayName("Фильтрация по статусу (hasStatus)")
  class HasStatusTests {

    @Test
    @DisplayName("PENDING — только ожидающие заявки")
    void filterByPending() {
      List<PaymentVerification> result =
          repository.findAll(Specification.where(hasStatus(VerificationStatus.PENDING)));

      assertThat(result).extracting(PaymentVerification::getId)
          .containsExactly(pendingIvan.getId());
    }

    @Test
    @DisplayName("APPROVED — только одобренные заявки")
    void filterByApproved() {
      List<PaymentVerification> result =
          repository.findAll(Specification.where(hasStatus(VerificationStatus.APPROVED)));

      assertThat(result).extracting(PaymentVerification::getId)
          .containsExactly(approvedPetr.getId());
    }

    @Test
    @DisplayName("REJECTED — только отклонённые заявки")
    void filterByRejected() {
      List<PaymentVerification> result =
          repository.findAll(Specification.where(hasStatus(VerificationStatus.REJECTED)));

      assertThat(result).extracting(PaymentVerification::getId)
          .containsExactly(rejectedAnna.getId());
    }

    @Test
    @DisplayName("null статус — все заявки")
    void nullStatusReturnsAll() {
      List<PaymentVerification> result =
          repository.findAll(Specification.where(hasStatus(null)));

      assertThat(result).hasSize(3);
    }
  }

  @Nested
  @DisplayName("Поиск по ФИО (fullNameContains)")
  class FullNameContainsTests {

    @Test
    @DisplayName("Поиск по user.fullName — регистронезависимо")
    void searchByUserFullName() {
      List<PaymentVerification> result =
          repository.findAll(Specification.where(fullNameContains("ivan")));

      assertThat(result).extracting(PaymentVerification::getId)
          .containsExactly(pendingIvan.getId());
    }

    @Test
    @DisplayName("Поиск по payerFullName")
    void searchByPayerFullName() {
      List<PaymentVerification> result =
          repository.findAll(Specification.where(fullNameContains("sidorov p")));

      assertThat(result).extracting(PaymentVerification::getId)
          .containsExactly(approvedPetr.getId());
    }

    @Test
    @DisplayName("LIKE-метасимволы в запросе экранируются")
    void likeMetacharactersAreEscaped() {
      List<PaymentVerification> result =
          repository.findAll(Specification.where(fullNameContains("%")));

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Пустой или null поиск — все заявки")
    void nullOrBlankReturnsAll() {
      assertThat(repository.findAll(Specification.where(fullNameContains(null)))).hasSize(3);
      assertThat(repository.findAll(Specification.where(fullNameContains("")))).hasSize(3);
      assertThat(repository.findAll(Specification.where(fullNameContains("   ")))).hasSize(3);
    }
  }

  @Nested
  @DisplayName("Фильтрация по createdAt")
  class CreatedAtRangeTests {

    @Test
    @DisplayName("createdAtFrom отсекает более старые заявки")
    void createdAtFromFiltersOlder() {
      Instant from = Instant.now().minus(36, ChronoUnit.HOURS);

      List<PaymentVerification> result =
          repository.findAll(Specification.where(createdAtFrom(from)));

      assertThat(result).extracting(PaymentVerification::getId)
          .containsExactlyInAnyOrder(approvedPetr.getId(), rejectedAnna.getId());
    }

    @Test
    @DisplayName("createdAtTo отсекает более новые заявки")
    void createdAtToFiltersNewer() {
      Instant to = Instant.now().minus(30, ChronoUnit.HOURS);

      List<PaymentVerification> result =
          repository.findAll(Specification.where(createdAtTo(to)));

      assertThat(result).extracting(PaymentVerification::getId)
          .containsExactly(pendingIvan.getId());
    }
  }

  @Nested
  @DisplayName("Комбинированные фильтры и сортировка")
  class CombinedTests {

    @Test
    @DisplayName("Статус + ФИО")
    void statusAndFullName() {
      Specification<PaymentVerification> spec = Specification
          .where(hasStatus(VerificationStatus.PENDING))
          .and(fullNameContains("petrov"));

      List<PaymentVerification> result = repository.findAll(spec);

      assertThat(result).extracting(PaymentVerification::getId)
          .containsExactly(pendingIvan.getId());
    }

    @Test
    @DisplayName("Сортировка по createdAt DESC + пагинация")
    void sortByCreatedAtDescWithPagination() {
      Page<PaymentVerification> page = repository.findAll(
          Specification.where(hasStatus(null)),
          PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

      assertThat(page.getContent()).hasSize(2);
      assertThat(page.getTotalElements()).isEqualTo(3);
      assertThat(page.getContent().get(0).getId()).isEqualTo(rejectedAnna.getId());
      assertThat(page.getContent().get(1).getId()).isEqualTo(approvedPetr.getId());
    }

    @Test
    @DisplayName("Сортировка по user.fullName ASC")
    void sortByUserFullNameAsc() {
      Page<PaymentVerification> page = repository.findAll(
          Specification.where(hasStatus(null)),
          PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "user.fullName")));

      assertThat(page.getContent()).extracting(v -> v.getUser().getFullName())
          .containsExactly("Anna Smirnova", "Ivan Petrov", "Petr Sidorov");
    }
  }

  private User persistUser(String fullName) {
    User user = new User();
    user.setFullName(fullName);
    user.setRole(UserRole.REGULAR);
    return em.persist(user);
  }

  private PaymentVerification persistVerification(
      User user,
      String payerFullName,
      VerificationStatus status,
      Instant createdAt,
      String paidUntilDate) {
    PaymentVerification verification = new PaymentVerification();
    verification.setUser(user);
    verification.setPayerFullName(payerFullName);
    verification.setPaidAmount(BigDecimal.valueOf(1500.00));
    verification.setCurrency("RUB");
    verification.setStatus(status);
    verification.setCreatedAt(createdAt);
    verification.setPaidUntilDate(paidUntilDate);
    return em.persist(verification);
  }
}
