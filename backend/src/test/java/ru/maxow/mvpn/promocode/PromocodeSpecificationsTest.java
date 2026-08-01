package ru.maxow.mvpn.promocode;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.maxow.mvpn.promocode.PromocodeSpecifications.codeContains;
import static ru.maxow.mvpn.promocode.PromocodeSpecifications.hasStatus;

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
import ru.maxow.mvpn.model.PromocodeStatus;
import ru.maxow.mvpn.tariff.Tariff;

@DataJpaTest
@DisplayName("PromocodeSpecifications - Интеграционные тесты")
class PromocodeSpecificationsTest {

  @Autowired
  private PromocodeRepository promocodeRepository;

  @Autowired
  private TestEntityManager em;

  @BeforeEach
  void setUp() {
    Tariff tariff = new Tariff();
    tariff.setName("Standard");
    tariff.setMaxDevices(5);
    tariff.setTrafficLimitGb(100);
    tariff.setDurationOfDays(30);
    em.persist(tariff);

    Promocode active = new Promocode();
    active.setCode("ABC12345");
    active.setExpirationDate(OffsetDateTime.now().plusDays(7));
    active.setTariff(tariff);
    active.setUsageLimit(3);
    active.setUsage(1);
    active.setStatus(PromocodeStatus.ACTIVE);
    em.persist(active);

    Promocode used = new Promocode();
    used.setCode("QWER5678");
    used.setExpirationDate(OffsetDateTime.now().plusDays(7));
    used.setTariff(tariff);
    used.setUsageLimit(2);
    used.setUsage(2);
    used.setStatus(PromocodeStatus.USED);
    em.persist(used);

    Promocode expired = new Promocode();
    expired.setCode("ZXCV9999");
    expired.setExpirationDate(OffsetDateTime.now().minusDays(1));
    expired.setTariff(tariff);
    expired.setUsageLimit(1);
    expired.setUsage(1);
    expired.setStatus(PromocodeStatus.EXPIRED);
    em.persist(expired);

    em.flush();
  }

  @Nested
  @DisplayName("Фильтрация по статусу")
  class HasStatusTests {

    @Test
    @DisplayName("ACTIVE — возвращает только активные промокоды")
    void filterByActiveStatus() {
      List<Promocode> result = promocodeRepository.findAll(Specification.where(hasStatus("ACTIVE")));

      assertThat(result).extracting(Promocode::getCode).containsExactly("ABC12345");
    }

    @Test
    @DisplayName("USED — возвращает только использованные промокоды")
    void filterByUsedStatus() {
      List<Promocode> result = promocodeRepository.findAll(Specification.where(hasStatus("USED")));

      assertThat(result).extracting(Promocode::getCode).containsExactly("QWER5678");
    }

    @Test
    @DisplayName("EXPIRED — возвращает только истёкшие промокоды")
    void filterByExpiredStatus() {
      List<Promocode> result = promocodeRepository.findAll(Specification.where(hasStatus("EXPIRED")));

      assertThat(result).extracting(Promocode::getCode).containsExactly("ZXCV9999");
    }

    @Test
    @DisplayName("Пустой или null статус — возвращает все промокоды")
    void nullOrBlankStatusReturnsAll() {
      assertThat(promocodeRepository.findAll(Specification.where(hasStatus(null)))).hasSize(3);
      assertThat(promocodeRepository.findAll(Specification.where(hasStatus("")))).hasSize(3);
      assertThat(promocodeRepository.findAll(Specification.where(hasStatus("   ")))).hasSize(3);
    }

    @Test
    @DisplayName("Неизвестный статус — возвращает пустой результат")
    void invalidStatusReturnsEmpty() {
      List<Promocode> result = promocodeRepository.findAll(Specification.where(hasStatus("UNKNOWN")));

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Поиск по коду")
  class CodeContainsTests {

    @Test
    @DisplayName("Поиск по части кода — регистронезависимый")
    void searchByPartialCode() {
      List<Promocode> result = promocodeRepository.findAll(Specification.where(codeContains("abc")));

      assertThat(result).extracting(Promocode::getCode).containsExactly("ABC12345");
    }

    @Test
    @DisplayName("Поиск по несуществующей строке — пустой результат")
    void searchByNonExistentString() {
      List<Promocode> result = promocodeRepository.findAll(Specification.where(codeContains("zzzzz")));

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Пустой или null поиск — возвращает все промокоды")
    void nullOrBlankSearchReturnsAll() {
      assertThat(promocodeRepository.findAll(Specification.where(codeContains(null)))).hasSize(3);
      assertThat(promocodeRepository.findAll(Specification.where(codeContains("")))).hasSize(3);
      assertThat(promocodeRepository.findAll(Specification.where(codeContains("   ")))).hasSize(3);
    }
  }

  @Nested
  @DisplayName("Комбинированные фильтры и сортировка")
  class CombinedFilterTests {

    @Test
    @DisplayName("Статус + поиск — находит нужный промокод")
    void statusAndSearch() {
      Specification<Promocode> spec = Specification.where(hasStatus("ACTIVE"))
          .and(codeContains("abc"));

      List<Promocode> result = promocodeRepository.findAll(spec);

      assertThat(result).extracting(Promocode::getCode).containsExactly("ABC12345");
    }

    @Test
    @DisplayName("Фильтр + пагинация + сортировка — корректно возвращает страницу")
    void filterWithPaginationAndSorting() {
      Page<Promocode> page = promocodeRepository.findAll(
          Specification.where(hasStatus(null)),
          PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "code")));

      assertThat(page.getContent()).extracting(Promocode::getCode)
          .containsExactly("ZXCV9999", "QWER5678");
      assertThat(page.getTotalElements()).isEqualTo(3);
      assertThat(page.getTotalPages()).isEqualTo(2);
      assertThat(page.getNumber()).isEqualTo(0);
      assertThat(page.getSize()).isEqualTo(2);
    }
  }
}



