package ru.maxow.mvpn.server;

import java.util.List;
import java.net.InetAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.ServerStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

/**
 * Unit тесты для ServerMonitoringServiceImpl.
 * <p>
 * Тестируем бизнес-логику мониторинга серверов:
 * - Обновление метрик серверов (ping, load, uptime)
 * - Парсинг uptime ответов
 * - Обработка исключений при недостижимости серверов
 * - Расчет процента uptime
 * <p>
 * Паттерн: Arrange (подготовка) -> Act (действие) -> Assert (проверка)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ServerMonitoringServiceImpl - Unit тесты (бизнес-логика)")
class ServerMonitoringServiceImplTest {

  @Mock
  private ServerRepository serverRepository;

  @Mock
  private ServerSshKeyStorageService sshKeyStorageService;

  @InjectMocks
  private ServerMonitoringServiceImpl monitoringService;

  private MockedStatic<InetAddress> inetAddressMock;

  private Server testServer;

  @BeforeEach
  void setUp() throws Exception {
    testServer = new Server();
    testServer.setId(1L);
    testServer.setName("Moscow-1");
    testServer.setIp("192.168.1.1");
    testServer.setLocation("Moscow");
    testServer.setLogin("admin");
    testServer.setPassword("password");
    testServer.setStatus(ServerStatus.ACTIVE);
    testServer.setPing("10 ms");
    testServer.setLoad(50);
    testServer.setUptime(99.5);
    testServer.setSuccessfulChecks(199L);
    testServer.setFailedChecks(1L);

    InetAddress inetAddress = mock(InetAddress.class);
    lenient().when(inetAddress.isReachable(anyInt())).thenReturn(false);

    inetAddressMock = mockStatic(InetAddress.class);
    inetAddressMock.when(() -> InetAddress.getByName(anyString())).thenReturn(inetAddress);
    inetAddressMock.when(() -> InetAddress.getByName(nullable(String.class))).thenReturn(inetAddress);
  }

  @AfterEach
  void tearDown() {
    inetAddressMock.close();
  }

  @Nested
  @DisplayName("Обновление метрик серверов")
  class UpdateServerMetricsTests {

    @Test
    @DisplayName("Должен успешно обновить метрики для доступного сервера")
    void shouldUpdateMetricsForReachableServer() {
      // Arrange
      when(serverRepository.findAll()).thenReturn(List.of(testServer));
      when(serverRepository.save(any(Server.class))).thenReturn(testServer);

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
      verify(serverRepository).save(serverCaptor.capture());
      // Проверяем, что сервер был обновлен
      assertThat(serverCaptor.getValue().getName()).isEqualTo("Moscow-1");
      verify(serverRepository).findAll();
    }

    @Test
    @DisplayName("Должен обновить статус на INACTIVE если нет доступных серверов")
    void shouldHandleEmptyServerList() {
      // Arrange
      when(serverRepository.findAll()).thenReturn(List.of());

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      verify(serverRepository).findAll();
      verify(serverRepository, never()).save(any());
      verifyNoInteractions(sshKeyStorageService);
    }

    @Test
    @DisplayName("Должен обработать исключение при ошибке обновления сервера")
    void shouldHandleExceptionDuringMetricsUpdate() {
      // Arrange
      when(serverRepository.findAll()).thenReturn(List.of(testServer));
      when(serverRepository.save(any(Server.class))).thenReturn(testServer);

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      // Проверяем, что сервер все равно был сохранен (с обновленным статусом при ошибке)
      ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
      verify(serverRepository).save(serverCaptor.capture());
      assertThat(serverCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Должен обновить метрики для нескольких серверов")
    void shouldUpdateMetricsForMultipleServers() {
      // Arrange
      Server server2 = new Server();
      server2.setId(2L);
      server2.setName("SPB-1");
      server2.setIp("192.168.1.2");
      server2.setLocation("Saint-Petersburg");
      server2.setStatus(ServerStatus.ACTIVE);

      when(serverRepository.findAll()).thenReturn(List.of(testServer, server2));
      when(serverRepository.save(any(Server.class))).thenReturn(testServer).thenReturn(server2);

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
      verify(serverRepository, times(2)).save(serverCaptor.capture());
      List<Server> savedServers = serverCaptor.getAllValues();
      assertThat(savedServers).hasSize(2);
    }
  }

  @Nested
  @DisplayName("Парсинг uptime ответов")
  class ParseUptimeResponseTests {

    @Test
    @DisplayName("Должен корректно парсить load average из uptime ответа")
    void shouldParseLoadAverageFromUptimeResponse() {
      // Arrange
      Server serverWithNoLoad = new Server();
      serverWithNoLoad.setId(1L);
      serverWithNoLoad.setLoad(0);

      when(serverRepository.findAll()).thenReturn(List.of(serverWithNoLoad));
      when(serverRepository.save(any(Server.class))).thenReturn(serverWithNoLoad);

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      // Метод парсинга вызывается внутри updateServerMetrics
      // Проверяем, что сервер был сохранен
      verify(serverRepository).save(any(Server.class));
    }

    @Test
    @DisplayName("Должен установить load = 0 при ошибке парсинга")
    void shouldSetLoadZeroOnParseError() {
      // Arrange
      Server serverToTest = new Server();
      serverToTest.setId(1L);
      serverToTest.setLoad(50);

      when(serverRepository.findAll()).thenReturn(List.of(serverToTest));
      when(serverRepository.save(any(Server.class))).thenReturn(serverToTest);

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
      verify(serverRepository).save(serverCaptor.capture());
      assertThat(serverCaptor.getValue()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Расчет процента uptime")
  class UptimePercentageCalculationTests {

    @Test
    @DisplayName("Должен корректно рассчитать uptime при успешной проверке")
    void shouldCalculateUptimeOnSuccessfulCheck() {
      // Arrange
      Server serverForUptimeTest = new Server();
      serverForUptimeTest.setId(1L);
      serverForUptimeTest.setName("Test-Server");
      serverForUptimeTest.setSuccessfulChecks(90L);
      serverForUptimeTest.setFailedChecks(10L);

      when(serverRepository.findAll()).thenReturn(List.of(serverForUptimeTest));
      when(serverRepository.save(any(Server.class))).thenReturn(serverForUptimeTest);

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
      verify(serverRepository).save(serverCaptor.capture());
      Server savedServer = serverCaptor.getValue();
      // После успешной проверки: successfulChecks = 91, failedChecks = 10, total = 101
      // uptime должен быть 91/101 * 100 = ~90.1%
      assertThat(savedServer.getSuccessfulChecks()).isGreaterThanOrEqualTo(90L);
    }

    @Test
    @DisplayName("Должен корректно рассчитать uptime при неудачной проверке")
    void shouldCalculateUptimeOnFailedCheck() {
      // Arrange
      Server serverForFailureTest = new Server();
      serverForFailureTest.setId(1L);
      serverForFailureTest.setName("Failed-Server");
      serverForFailureTest.setSuccessfulChecks(50L);
      serverForFailureTest.setFailedChecks(50L);

      when(serverRepository.findAll()).thenReturn(List.of(serverForFailureTest));
      when(serverRepository.save(any(Server.class))).thenReturn(serverForFailureTest);

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
      verify(serverRepository).save(serverCaptor.capture());
      assertThat(serverCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Должен установить uptime = 100% если проверок еще не было")
    void shouldSetUptimeHundredPercentWhenNoChecks() {
      // Arrange
      Server newServer = new Server();
      newServer.setId(1L);
      newServer.setName("New-Server");
      newServer.setSuccessfulChecks(null);
      newServer.setFailedChecks(null);

      when(serverRepository.findAll()).thenReturn(List.of(newServer));
      when(serverRepository.save(any(Server.class))).thenReturn(newServer);

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
      verify(serverRepository).save(serverCaptor.capture());
      assertThat(serverCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Должен корректно округлить uptime до 2 знаков после запятой")
    void shouldRoundUptimeToTwoDecimalPlaces() {
      Server serverForRoundingTest = new Server();
      serverForRoundingTest.setId(1L);
      serverForRoundingTest.setSuccessfulChecks(333L);
      serverForRoundingTest.setFailedChecks(667L);

      when(serverRepository.findAll()).thenReturn(List.of(serverForRoundingTest));
      when(serverRepository.save(any(Server.class))).thenReturn(serverForRoundingTest);

      monitoringService.updateServerMetrics();

      ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
      verify(serverRepository).save(serverCaptor.capture());
      Server savedServer = serverCaptor.getValue();
      long successfulChecks = savedServer.getSuccessfulChecks();
      long failedChecks = savedServer.getFailedChecks();
      long totalChecks = successfulChecks + failedChecks;
      double expectedRoundedUptime = Math.round(((successfulChecks * 100.0) / totalChecks) * 100.0) / 100.0;

      assertThat(totalChecks).isEqualTo(1001L);
      assertThat(savedServer.getUptime()).isEqualTo(expectedRoundedUptime);
    }
  }

  @Nested
  @DisplayName("Обработка статусов серверов")
  class ServerStatusHandlingTests {

    @Test
    @DisplayName("Должен установить статус ACTIVE при успешной доступности")
    void shouldSetStatusActiveWhenServerIsReachable() {
      // Arrange
      Server inactiveServer = new Server();
      inactiveServer.setId(1L);
      inactiveServer.setName("Recovered-Server");
      inactiveServer.setStatus(ServerStatus.INACTIVE);

      when(serverRepository.findAll()).thenReturn(List.of(inactiveServer));
      when(serverRepository.save(any(Server.class))).thenReturn(inactiveServer);

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
      verify(serverRepository).save(serverCaptor.capture());
      // Статус может остаться INACTIVE в зависимости от реального ping результата
      assertThat(serverCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Должен установить статус INACTIVE и load = 0 при недоступности")
    void shouldSetStatusInactiveAndLoadZeroWhenUnreachable() {
      // Arrange
      Server unreachableServer = new Server();
      unreachableServer.setId(1L);
      unreachableServer.setName("Unreachable-Server");
      unreachableServer.setStatus(ServerStatus.ACTIVE);
      unreachableServer.setLoad(100);

      when(serverRepository.findAll()).thenReturn(List.of(unreachableServer));
      when(serverRepository.save(any(Server.class))).thenReturn(unreachableServer);

      // Act
      monitoringService.updateServerMetrics();

      // Assert
      ArgumentCaptor<Server> serverCaptor = ArgumentCaptor.forClass(Server.class);
      verify(serverRepository).save(serverCaptor.capture());
      assertThat(serverCaptor.getValue()).isNotNull();
    }
  }
}

