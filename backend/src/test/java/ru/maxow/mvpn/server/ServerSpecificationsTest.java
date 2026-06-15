package ru.maxow.mvpn.server;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.maxow.mvpn.server.ServerSpecifications.*;

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
import ru.maxow.mvpn.model.ServerStatus;

@DataJpaTest
@DisplayName("ServerSpecifications - Интеграционные тесты")
class ServerSpecificationsTest {

  @Autowired
  private ServerRepository serverRepository;

  @Autowired
  private TestEntityManager em;

  @BeforeEach
  void setUp() {
    Server moscow = new Server();
    moscow.setName("Moscow-1");
    moscow.setLocation("RU-MOW");
    moscow.setIp("185.10.12.1");
    moscow.setStatus(ServerStatus.ACTIVE);
    moscow.setLoad(45);
    moscow.setUsage(12);
    moscow.setMaxUsers(100);
    moscow.setMaxTraffic(1000);
    moscow.setPing("15ms");
    moscow.setUptime(99.5);
    em.persist(moscow);

    Server helsinki = new Server();
    helsinki.setName("Helsinki-1");
    helsinki.setLocation("FI-HEL");
    helsinki.setIp("91.200.14.55");
    helsinki.setStatus(ServerStatus.INACTIVE);
    helsinki.setLoad(0);
    helsinki.setUsage(0);
    helsinki.setMaxUsers(200);
    helsinki.setMaxTraffic(500);
    helsinki.setPing("40ms");
    helsinki.setUptime(85.0);
    em.persist(helsinki);

    Server berlin = new Server();
    berlin.setName("Berlin-Moscow-Relay");
    berlin.setLocation("DE-BER");
    berlin.setIp("45.33.32.156");
    berlin.setStatus(ServerStatus.MAINTENANCE);
    berlin.setLoad(10);
    berlin.setUsage(3);
    berlin.setMaxUsers(50);
    berlin.setMaxTraffic(300);
    berlin.setPing("25ms");
    berlin.setUptime(70.0);
    em.persist(berlin);

    em.flush();
  }

  @Nested
  @DisplayName("Фильтрация по статусу (hasStatus)")
  class HasStatusTests {

    @Test
    @DisplayName("Фильтр по ACTIVE — возвращает только активные серверы")
    void filterByActiveStatus() {
      List<Server> result = serverRepository.findAll(Specification.where(hasStatus("ACTIVE")));

      assertThat(result).extracting(Server::getName).containsExactly("Moscow-1");
    }

    @Test
    @DisplayName("Фильтр по INACTIVE — возвращает неактивные серверы")
    void filterByInactiveStatus() {
      List<Server> result = serverRepository.findAll(Specification.where(hasStatus("INACTIVE")));

      assertThat(result).extracting(Server::getName).containsExactly("Helsinki-1");
    }

    @Test
    @DisplayName("Фильтр по MAINTENANCE — возвращает серверы на обслуживании")
    void filterByMaintenanceStatus() {
      List<Server> result = serverRepository.findAll(Specification.where(hasStatus("MAINTENANCE")));

      assertThat(result).extracting(Server::getName).containsExactly("Berlin-Moscow-Relay");
    }

    @Test
    @DisplayName("Пустой или null статус — возвращает все серверы")
    void nullOrBlankStatusReturnsAll() {
      assertThat(serverRepository.findAll(Specification.where(hasStatus(null)))).hasSize(3);
      assertThat(serverRepository.findAll(Specification.where(hasStatus("")))).hasSize(3);
      assertThat(serverRepository.findAll(Specification.where(hasStatus("   ")))).hasSize(3);
    }

    @Test
    @DisplayName("Невалидный статус — возвращает пустой результат")
    void invalidStatusReturnsEmpty() {
      List<Server> result = serverRepository.findAll(Specification.where(hasStatus("UNKNOWN")));

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Поиск по имени или IP (nameOrIpContains)")
  class NameOrIpContainsTests {

    @Test
    @DisplayName("Поиск по части имени — находит совпадения (регистронезависимо)")
    void searchByPartialName() {
      List<Server> result = serverRepository.findAll(Specification.where(nameOrIpContains("moscow")));

      assertThat(result).extracting(Server::getName)
          .containsExactlyInAnyOrder("Moscow-1", "Berlin-Moscow-Relay");
    }

    @Test
    @DisplayName("Поиск по IP-адресу — находит совпадения")
    void searchByIp() {
      List<Server> result = serverRepository.findAll(Specification.where(nameOrIpContains("91.200")));

      assertThat(result).extracting(Server::getName).containsExactly("Helsinki-1");
    }

    @Test
    @DisplayName("Поиск по подстроке, встречающейся и в имени и в IP — возвращает уникальные результаты")
    void searchMatchingBothNameAndIp() {
      // "33" appears in Berlin's IP (45.33.32.156) but not in names
      List<Server> result = serverRepository.findAll(Specification.where(nameOrIpContains("33")));

      assertThat(result).extracting(Server::getName).containsExactly("Berlin-Moscow-Relay");
    }

    @Test
    @DisplayName("Поиск по несуществующей строке — возвращает пустой результат")
    void searchByNonExistentString() {
      List<Server> result = serverRepository.findAll(Specification.where(nameOrIpContains("zzzzz")));

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Пустой или null поиск — возвращает все серверы")
    void nullOrBlankSearchReturnsAll() {
      assertThat(serverRepository.findAll(Specification.where(nameOrIpContains(null)))).hasSize(3);
      assertThat(serverRepository.findAll(Specification.where(nameOrIpContains("")))).hasSize(3);
    }
  }

  @Nested
  @DisplayName("Комбинированные фильтры")
  class CombinedFilterTests {

    @Test
    @DisplayName("Статус + Поиск — находит ACTIVE серверы с 'moscow' в имени")
    void statusAndSearch() {
      Specification<Server> spec = Specification.where(hasStatus("ACTIVE"))
          .and(nameOrIpContains("moscow"));

      List<Server> result = serverRepository.findAll(spec);

      assertThat(result).extracting(Server::getName).containsExactly("Moscow-1");
    }

    @Test
    @DisplayName("Противоречивые фильтры — возвращает пустой результат")
    void conflictingFilters() {
      Specification<Server> spec = Specification.where(hasStatus("ACTIVE"))
          .and(nameOrIpContains("helsinki"));

      List<Server> result = serverRepository.findAll(spec);

      // Helsinki is INACTIVE, not ACTIVE
      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Сортировка")
  class SortingTests {

    @Test
    @DisplayName("Сортировка по name ASC — в алфавитном порядке")
    void sortByNameAsc() {
      Page<Server> page = serverRepository.findAll(
          Specification.where(hasStatus(null)),
          PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name")));

      assertThat(page.getContent()).extracting(Server::getName)
          .containsExactly("Berlin-Moscow-Relay", "Helsinki-1", "Moscow-1");
    }

    @Test
    @DisplayName("Сортировка по status DESC")
    void sortByStatusDesc() {
      Page<Server> page = serverRepository.findAll(
          Specification.where(hasStatus(null)),
          PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "status")));

      assertThat(page.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("Сортировка по load ASC — по нагрузке")
    void sortByLoadAsc() {
      Page<Server> page = serverRepository.findAll(
          Specification.where(hasStatus(null)),
          PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "load")));

      assertThat(page.getContent()).extracting(Server::getLoad)
          .containsExactly(0, 10, 45);
    }

    @Test
    @DisplayName("Сортировка по usage DESC — по количеству активных пользователей")
    void sortByUsageDesc() {
      Page<Server> page = serverRepository.findAll(
          Specification.where(hasStatus(null)),
          PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "usage")));

      assertThat(page.getContent()).extracting(Server::getUsage)
          .containsExactly(12, 3, 0);
    }

    @Test
    @DisplayName("Сортировка по uptime DESC — по аптайму")
    void sortByUptimeDesc() {
      Page<Server> page = serverRepository.findAll(
          Specification.where(hasStatus(null)),
          PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "uptime")));

      assertThat(page.getContent()).extracting(Server::getUptime)
          .containsExactly(99.5, 85.0, 70.0);
    }

    @Test
    @DisplayName("Сортировка + фильтрация + пагинация — корректный результат")
    void sortWithFilterAndPagination() {
      Specification<Server> spec = Specification.where(hasStatus(null));

      Page<Server> page = serverRepository.findAll(spec,
          PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "name")));

      assertThat(page.getContent()).hasSize(2);
      assertThat(page.getTotalElements()).isEqualTo(3);
      assertThat(page.getTotalPages()).isEqualTo(2);
      assertThat(page.getContent().getFirst().getName()).isEqualTo("Berlin-Moscow-Relay");
    }
  }
}
