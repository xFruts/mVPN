package ru.maxow.mvpn.server;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.maxow.mvpn.model.ServerStatus;

import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServerMonitoringServiceImpl - deterministic unit tests")
class ServerMonitoringServiceImplDeterministicTest {

  @Mock
  private ServerRepository serverRepository;

  @InjectMocks
  private ServerMonitoringServiceImpl service;

  @Test
  @DisplayName("Given reachable server and valid uptime response When updateServerMetrics Then set ACTIVE and parsed load")
  void givenReachableServerWhenUpdateMetricsThenSetActiveAndLoad() throws Exception {
    Server server = server("10.0.0.1");
    server.setStatus(ServerStatus.INACTIVE);
    server.setSuccessfulChecks(2L);
    server.setFailedChecks(1L);

    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(true);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class);
         MockedConstruction<JSch> ignored = mockConstruction(JSch.class, (jsch, context) -> {
           Session session = mock(Session.class);
           ChannelExec channel = mock(ChannelExec.class);
           AtomicReference<OutputStream> outputRef = new AtomicReference<>();

           when(jsch.getSession("admin", "10.0.0.1", 22)).thenReturn(session);
           doNothing().when(session).setPassword("pwd");
           doNothing().when(session).connect(anyInt());
           when(session.openChannel("exec")).thenReturn(channel);

           doNothing().when(channel).setCommand("uptime");
           doAnswer(invocation -> {
             outputRef.set(invocation.getArgument(0));
             return null;
           }).when(channel).setOutputStream(any(OutputStream.class));
           doAnswer(invocation -> {
             outputRef.get().write("load average: 0.42, 0.31, 0.25".getBytes(StandardCharsets.UTF_8));
             return null;
           }).when(channel).connect();
           when(channel.isConnected()).thenReturn(false);
         })) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.1")).thenReturn(inetAddress);

      service.updateServerMetrics();
    }

    ArgumentCaptor<Server> captor = ArgumentCaptor.forClass(Server.class);
    verify(serverRepository).save(captor.capture());
    Server saved = captor.getValue();

    assertThat(saved.getStatus()).isEqualTo(ServerStatus.ACTIVE);
    assertThat(saved.getPing()).contains("ms");
    assertThat(saved.getLoad()).isEqualTo(42);
    assertThat(saved.getSuccessfulChecks()).isEqualTo(3L);
    assertThat(saved.getFailedChecks()).isEqualTo(1L);
    assertThat(saved.getUptime()).isEqualTo(75.0);
  }

  @Test
  @DisplayName("Given unreachable server When updateServerMetrics Then set INACTIVE and fallback metrics")
  void givenUnreachableServerWhenUpdateMetricsThenSetInactive() throws Exception {
    Server server = server("10.0.0.2");
    server.setStatus(ServerStatus.ACTIVE);
    server.setLoad(99);
    server.setSuccessfulChecks(5L);
    server.setFailedChecks(0L);

    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(false);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class)) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.2")).thenReturn(inetAddress);
      service.updateServerMetrics();
    }

    ArgumentCaptor<Server> captor = ArgumentCaptor.forClass(Server.class);
    verify(serverRepository).save(captor.capture());
    Server saved = captor.getValue();

    assertThat(saved.getStatus()).isEqualTo(ServerStatus.INACTIVE);
    assertThat(saved.getPing()).isEqualTo("N/A");
    assertThat(saved.getLoad()).isZero();
    assertThat(saved.getSuccessfulChecks()).isEqualTo(5L);
    assertThat(saved.getFailedChecks()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Given SSH error on reachable server When updateServerMetrics Then catch and persist fallback values")
  void givenSshFailureWhenUpdateMetricsThenPersistFallbackValues() throws Exception {
    Server server = server("10.0.0.3");
    server.setSuccessfulChecks(1L);
    server.setFailedChecks(1L);

    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(true);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class);
         MockedConstruction<JSch> ignored = mockConstruction(JSch.class, (jsch, context) -> {
           Session session = mock(Session.class);
           when(jsch.getSession("admin", "10.0.0.3", 22)).thenReturn(session);
           doThrow(new RuntimeException("ssh down")).when(session).connect(anyInt());
         })) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.3")).thenReturn(inetAddress);
      service.updateServerMetrics();
    }

    ArgumentCaptor<Server> captor = ArgumentCaptor.forClass(Server.class);
    verify(serverRepository).save(captor.capture());
    Server saved = captor.getValue();

    assertThat(saved.getStatus()).isEqualTo(ServerStatus.INACTIVE);
    assertThat(saved.getPing()).isEqualTo("N/A");
    assertThat(saved.getLoad()).isZero();
    assertThat(saved.getSuccessfulChecks()).isEqualTo(1L);
    assertThat(saved.getFailedChecks()).isEqualTo(2L);
  }

  @Test
  @DisplayName("Given malformed uptime response When parseUptimeResponse Then set load to zero")
  void givenMalformedUptimeWhenParseThenLoadZero() {
    Server server = server("10.0.0.4");
    server.setLoad(77);

    ReflectionTestUtils.invokeMethod(service, "parseUptimeResponse", server, "load average: x, y, z");

    assertThat(server.getLoad()).isZero();
  }

  private Server server(String ip) {
    Server server = new Server();
    server.setId(1L);
    server.setName("test-server");
    server.setIp(ip);
    server.setLogin("admin");
    server.setPassword("pwd");
    server.setStatus(ServerStatus.ACTIVE);
    return server;
  }
}

