package ru.maxow.mvpn.server;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Focused unit tests for load parsing and uptime percentage calculation.
 * End-to-end SSH/ping flows live in {@link ServerMonitoringServiceImplDeterministicTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServerMonitoringServiceImpl - parse & uptime unit tests")
class ServerMonitoringServiceImplTest {

  @InjectMocks
  private ServerMonitoringServiceImpl monitoringService;

  @Nested
  @DisplayName("parseLoadResponse")
  class ParseLoadResponseTests {

    @ParameterizedTest(name = "loadavg={0} nproc={1} → {2}%")
    @CsvSource({
        "0.42, 1, 42",
        "1.86, 1, 100",
        "1.86, 4, 47",
        "0.00, 8, 0",
        "4.00, 4, 100",
        "0.50, 2, 25"
    })
    @DisplayName("Normalizes /proc/loadavg by nproc and clamps to 0–100")
    void shouldNormalizeLoadavgByNproc(String loadAvg, int nproc, int expected) {
      Server server = new Server();
      server.setName("srv");
      server.setLoad(99);

      String response = loadAvg + " 0.30 0.20 1/100 1\n" + nproc + "\n";
      monitoringService.parseLoadResponse(server, response);

      assertThat(server.getLoad()).isEqualTo(expected);
    }

    @Test
    @DisplayName("Accepts legacy uptime text and treats as 1 CPU (clamped)")
    void shouldParseLegacyUptimeText() {
      Server server = new Server();
      server.setName("srv");

      monitoringService.parseLoadResponse(
          server,
          " 12:00:00 up 1 day,  1 user,  load average: 1.86, 1.50, 1.20\n");

      assertThat(server.getLoad()).isEqualTo(100);
    }

    @Test
    @DisplayName("Accepts European comma decimal in legacy uptime text")
    void shouldParseEuropeanCommaDecimal() {
      Server server = new Server();
      server.setName("srv");

      monitoringService.parseLoadResponse(
          server,
          "load average: 0,42, 0,30, 0,20");

      assertThat(server.getLoad()).isEqualTo(42);
    }

    @Test
    @DisplayName("Sets load 0 on malformed response")
    void shouldSetLoadZeroOnMalformed() {
      Server server = new Server();
      server.setName("srv");
      server.setLoad(77);

      monitoringService.parseLoadResponse(server, "load average: x, y, z");

      assertThat(server.getLoad()).isZero();
    }

    @Test
    @DisplayName("Sets load 0 on blank response")
    void shouldSetLoadZeroOnBlank() {
      Server server = new Server();
      server.setName("srv");
      server.setLoad(50);

      monitoringService.parseLoadResponse(server, "   ");

      assertThat(server.getLoad()).isZero();
    }
  }

  @Nested
  @DisplayName("updateUptimePercentage")
  class UptimePercentageTests {

    @Test
    @DisplayName("Increments successful checks and recalculates uptime")
    void shouldUpdateOnSuccess() {
      Server server = new Server();
      server.setSuccessfulChecks(2L);
      server.setFailedChecks(1L);

      ReflectionTestUtils.invokeMethod(monitoringService, "updateUptimePercentage", server, true);

      assertThat(server.getSuccessfulChecks()).isEqualTo(3L);
      assertThat(server.getFailedChecks()).isEqualTo(1L);
      assertThat(server.getUptime()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("Increments failed checks and recalculates uptime")
    void shouldUpdateOnFailure() {
      Server server = new Server();
      server.setSuccessfulChecks(5L);
      server.setFailedChecks(0L);

      ReflectionTestUtils.invokeMethod(monitoringService, "updateUptimePercentage", server, false);

      assertThat(server.getSuccessfulChecks()).isEqualTo(5L);
      assertThat(server.getFailedChecks()).isEqualTo(1L);
      assertThat(server.getUptime()).isEqualTo(83.33);
    }

    @Test
    @DisplayName("Treats null counters as zero")
    void shouldTreatNullCountersAsZero() {
      Server server = new Server();
      server.setSuccessfulChecks(null);
      server.setFailedChecks(null);

      ReflectionTestUtils.invokeMethod(monitoringService, "updateUptimePercentage", server, true);

      assertThat(server.getSuccessfulChecks()).isEqualTo(1L);
      assertThat(server.getFailedChecks()).isEqualTo(0L);
      assertThat(server.getUptime()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Rounds uptime to two decimal places")
    void shouldRoundToTwoDecimals() {
      Server server = new Server();
      server.setSuccessfulChecks(333L);
      server.setFailedChecks(667L);

      ReflectionTestUtils.invokeMethod(monitoringService, "updateUptimePercentage", server, false);

      // after fail: 333 / 1001 * 100 = 33.266... → 33.27
      assertThat(server.getUptime()).isEqualTo(33.27);
    }
  }
}
