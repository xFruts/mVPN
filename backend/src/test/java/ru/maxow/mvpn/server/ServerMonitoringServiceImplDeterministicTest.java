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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import ru.maxow.mvpn.model.ServerStatus;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServerMonitoringServiceImpl - deterministic unit tests")
class ServerMonitoringServiceImplDeterministicTest {

  private static final String LOAD_CMD = "cat /proc/loadavg; nproc";

  @Mock
  private ServerRepository serverRepository;

  @Mock
  private ServerSshKeyStorageService sshKeyStorageService;

  @InjectMocks
  private ServerMonitoringServiceImpl service;

  @Test
  @DisplayName("Given reachable server When update Then ACTIVE, load % from loadavg/nproc, uptime updated")
  void givenReachableServerWhenUpdateMetricsThenSetActiveAndNormalizedLoad() throws Exception {
    Server server = server("10.0.0.1");
    server.setStatus(ServerStatus.INACTIVE);
    server.setSuccessfulChecks(2L);
    server.setFailedChecks(1L);

    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.existsById(1L)).thenReturn(true);
    when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(true);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class);
         MockedConstruction<JSch> ignored = mockConstruction(JSch.class, (jsch, context) -> {
           Session session = mock(Session.class);
           ChannelExec channel = mock(ChannelExec.class);
           InputStream loadResponse = new ByteArrayInputStream(
               "0.42 0.31 0.25 1/100 1\n1\n".getBytes(StandardCharsets.UTF_8));

           when(jsch.getSession("admin", "10.0.0.1", 22)).thenReturn(session);
           doNothing().when(session).setPassword("pwd");
           doNothing().when(session).connect(anyInt());
           when(session.openChannel("exec")).thenReturn(channel);

           doNothing().when(channel).setCommand(LOAD_CMD);
           when(channel.getInputStream()).thenReturn(loadResponse);
           doNothing().when(channel).connect();
           when(channel.isConnected()).thenReturn(false);
         })) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.1")).thenReturn(inetAddress);

      service.updateServersMetrics();
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
  @DisplayName("Given loadavg above nproc When update Then load capped at 100")
  void givenHighLoadWhenUpdateThenCapAt100() throws Exception {
    Server server = server("10.0.0.5");
    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.existsById(1L)).thenReturn(true);
    when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(true);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class);
         MockedConstruction<JSch> ignored = mockConstruction(JSch.class, (jsch, context) -> {
           Session session = mock(Session.class);
           ChannelExec channel = mock(ChannelExec.class);
           // 1.86 on 1 CPU → would be 186% with old formula; must cap at 100
           InputStream loadResponse = new ByteArrayInputStream(
               "1.86 1.50 1.20 1/100 1\n1\n".getBytes(StandardCharsets.UTF_8));

           when(jsch.getSession("admin", "10.0.0.5", 22)).thenReturn(session);
           doNothing().when(session).setPassword("pwd");
           doNothing().when(session).connect(anyInt());
           when(session.openChannel("exec")).thenReturn(channel);
           doNothing().when(channel).setCommand(LOAD_CMD);
           when(channel.getInputStream()).thenReturn(loadResponse);
           doNothing().when(channel).connect();
           when(channel.isConnected()).thenReturn(false);
         })) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.5")).thenReturn(inetAddress);
      service.updateServersMetrics();
    }

    ArgumentCaptor<Server> captor = ArgumentCaptor.forClass(Server.class);
    verify(serverRepository).save(captor.capture());
    assertThat(captor.getValue().getLoad()).isEqualTo(100);
  }

  @Test
  @DisplayName("Given multi-core host When update Then load is loadavg/nproc percent")
  void givenMultiCoreWhenUpdateThenNormalizeByCpuCount() throws Exception {
    Server server = server("10.0.0.6");
    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.existsById(1L)).thenReturn(true);
    when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(true);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class);
         MockedConstruction<JSch> ignored = mockConstruction(JSch.class, (jsch, context) -> {
           Session session = mock(Session.class);
           ChannelExec channel = mock(ChannelExec.class);
           // 1.86 / 4 ≈ 46.5 → 47
           InputStream loadResponse = new ByteArrayInputStream(
               "1.86 1.50 1.20 1/100 1\n4\n".getBytes(StandardCharsets.UTF_8));

           when(jsch.getSession("admin", "10.0.0.6", 22)).thenReturn(session);
           doNothing().when(session).setPassword("pwd");
           doNothing().when(session).connect(anyInt());
           when(session.openChannel("exec")).thenReturn(channel);
           doNothing().when(channel).setCommand(LOAD_CMD);
           when(channel.getInputStream()).thenReturn(loadResponse);
           doNothing().when(channel).connect();
           when(channel.isConnected()).thenReturn(false);
         })) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.6")).thenReturn(inetAddress);
      service.updateServersMetrics();
    }

    ArgumentCaptor<Server> captor = ArgumentCaptor.forClass(Server.class);
    verify(serverRepository).save(captor.capture());
    assertThat(captor.getValue().getLoad()).isEqualTo(47);
  }

  @Test
  @DisplayName("Given key auth server When update Then download key and connect without password")
  void givenKeyAuthServerWhenUpdateMetricsThenUsePrivateKey() throws Exception {
    Server server = server("10.0.0.10");
    server.setSshAuthType(SshAuthType.KEY);
    server.setSshPrivateKeyObjectKey("ssh-keys/server-10/id_ed25519");
    server.setPassword(null);

    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.existsById(1L)).thenReturn(true);
    when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(sshKeyStorageService.downloadPrivateKey("ssh-keys/server-10/id_ed25519"))
        .thenReturn("private-key".getBytes(StandardCharsets.UTF_8));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(true);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class);
         MockedConstruction<JSch> ignored = mockConstruction(JSch.class, (jsch, context) -> {
           Session session = mock(Session.class);
           ChannelExec channel = mock(ChannelExec.class);
           InputStream loadResponse = new ByteArrayInputStream(
               "0.10 0.10 0.10 1/100 1\n1\n".getBytes(StandardCharsets.UTF_8));

           when(jsch.getSession("admin", "10.0.0.10", 22)).thenReturn(session);
           doNothing().when(jsch).addIdentity(eq("server-1"), any(byte[].class), any(), any());
           doNothing().when(session).connect(anyInt());
           when(session.openChannel("exec")).thenReturn(channel);

           doNothing().when(channel).setCommand(LOAD_CMD);
           when(channel.getInputStream()).thenReturn(loadResponse);
           doNothing().when(channel).connect();
           when(channel.isConnected()).thenReturn(false);
         })) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.10")).thenReturn(inetAddress);
      service.updateServersMetrics();
    }

    verify(sshKeyStorageService).downloadPrivateKey("ssh-keys/server-10/id_ed25519");
  }

  @Test
  @DisplayName("Given unreachable server When update Then INACTIVE, load 0, failed check counted")
  void givenUnreachableServerWhenUpdateMetricsThenSetInactive() throws Exception {
    Server server = server("10.0.0.2");
    server.setStatus(ServerStatus.ACTIVE);
    server.setLoad(99);
    server.setSuccessfulChecks(5L);
    server.setFailedChecks(0L);

    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.existsById(1L)).thenReturn(true);
    when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(false);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class)) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.2")).thenReturn(inetAddress);
      service.updateServersMetrics();
    }

    ArgumentCaptor<Server> captor = ArgumentCaptor.forClass(Server.class);
    verify(serverRepository).save(captor.capture());
    verifyNoInteractions(sshKeyStorageService);
    Server saved = captor.getValue();

    assertThat(saved.getStatus()).isEqualTo(ServerStatus.INACTIVE);
    assertThat(saved.getPing()).isEqualTo("N/A");
    assertThat(saved.getLoad()).isZero();
    assertThat(saved.getSuccessfulChecks()).isEqualTo(5L);
    assertThat(saved.getFailedChecks()).isEqualTo(1L);
    assertThat(saved.getUptime()).isEqualTo(83.33);
  }

  @Test
  @DisplayName("Given SSH error on reachable server When update Then persist fallback values")
  void givenSshFailureWhenUpdateMetricsThenPersistFallbackValues() throws Exception {
    Server server = server("10.0.0.3");
    server.setSuccessfulChecks(1L);
    server.setFailedChecks(1L);

    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.existsById(1L)).thenReturn(true);
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
      service.updateServersMetrics();
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
  @DisplayName("Given key auth without private key object key When update Then persist fallback values")
  void givenKeyAuthWithoutObjectKeyWhenUpdateThenPersistFallbackValues() throws Exception {
    Server server = server("10.0.0.11");
    server.setSshAuthType(SshAuthType.KEY);
    server.setSshPrivateKeyObjectKey(" ");
    server.setSuccessfulChecks(4L);
    server.setFailedChecks(1L);

    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.existsById(1L)).thenReturn(true);
    when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(true);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class)) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.11")).thenReturn(inetAddress);
      service.updateServersMetrics();
    }

    ArgumentCaptor<Server> captor = ArgumentCaptor.forClass(Server.class);
    verify(serverRepository).save(captor.capture());
    verifyNoInteractions(sshKeyStorageService);
    Server saved = captor.getValue();

    assertThat(saved.getStatus()).isEqualTo(ServerStatus.INACTIVE);
    assertThat(saved.getPing()).isEqualTo("N/A");
    assertThat(saved.getLoad()).isZero();
    assertThat(saved.getSuccessfulChecks()).isEqualTo(4L);
    assertThat(saved.getFailedChecks()).isEqualTo(2L);
  }

  @Test
  @DisplayName("Given password auth without password When update Then persist fallback values")
  void givenPasswordAuthWithoutPasswordWhenUpdateThenPersistFallbackValues() throws Exception {
    Server server = server("10.0.0.12");
    server.setSshAuthType(SshAuthType.PASSWORD);
    server.setPassword(" ");
    server.setSuccessfulChecks(3L);
    server.setFailedChecks(0L);

    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.existsById(1L)).thenReturn(true);
    when(serverRepository.save(any(Server.class))).thenAnswer(invocation -> invocation.getArgument(0));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(true);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class);
         MockedConstruction<JSch> ignored = mockConstruction(JSch.class)) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.12")).thenReturn(inetAddress);
      service.updateServersMetrics();
    }

    ArgumentCaptor<Server> captor = ArgumentCaptor.forClass(Server.class);
    verify(serverRepository).save(captor.capture());
    verify(sshKeyStorageService, never()).downloadPrivateKey(any());
    Server saved = captor.getValue();

    assertThat(saved.getStatus()).isEqualTo(ServerStatus.INACTIVE);
    assertThat(saved.getPing()).isEqualTo("N/A");
    assertThat(saved.getLoad()).isZero();
    assertThat(saved.getSuccessfulChecks()).isEqualTo(3L);
    assertThat(saved.getFailedChecks()).isEqualTo(1L);
  }

  @Test
  @DisplayName("Given server deleted before save When update Then skip save")
  void givenServerDeletedBeforeSaveWhenUpdateThenSkipSave() throws Exception {
    Server server = server("10.0.0.20");
    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.existsById(1L)).thenReturn(false);

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(false);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class)) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.20")).thenReturn(inetAddress);
      service.updateServersMetrics();
    }

    verify(serverRepository, never()).save(any());
  }

  @Test
  @DisplayName("Given optimistic lock on save When update Then skip without rethrow")
  void givenOptimisticLockWhenUpdateThenSkip() throws Exception {
    Server server = server("10.0.0.21");
    when(serverRepository.findAll()).thenReturn(List.of(server));
    when(serverRepository.existsById(1L)).thenReturn(true);
    when(serverRepository.save(any(Server.class)))
        .thenThrow(new ObjectOptimisticLockingFailureException(Server.class, 1L));

    InetAddress inetAddress = mock(InetAddress.class);
    when(inetAddress.isReachable(5000)).thenReturn(false);

    try (MockedStatic<InetAddress> inetMock = mockStatic(InetAddress.class)) {
      inetMock.when(() -> InetAddress.getByName("10.0.0.21")).thenReturn(inetAddress);
      service.updateServersMetrics();
    }

    verify(serverRepository).save(any(Server.class));
  }

  @Test
  @DisplayName("Given empty server list When update Then no save")
  void givenEmptyListWhenUpdateThenNoSave() {
    when(serverRepository.findAll()).thenReturn(List.of());

    service.updateServersMetrics();

    verify(serverRepository, never()).existsById(anyLong());
    verify(serverRepository, never()).save(any());
    verifyNoInteractions(sshKeyStorageService);
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
