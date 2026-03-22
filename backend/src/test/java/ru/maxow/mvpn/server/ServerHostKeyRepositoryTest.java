package ru.maxow.mvpn.server;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServerHostKeyRepository - unit tests")
class ServerHostKeyRepositoryTest {

  @Mock
  private ServerRepository serverRepository;

  @Test
  @DisplayName("Given empty stored host key When check Then return OK")
  void givenEmptyStoredHostKeyWhenCheckThenOk() {
    Server server = new Server();
    server.setHostKey(null);

    ServerHostKeyRepository repository = new ServerHostKeyRepository(server, serverRepository);

    int result = repository.check("vpn-host", new byte[]{1, 2, 3});

    assertThat(result).isEqualTo(HostKeyRepository.OK);
  }

  @Test
  @DisplayName("Given matching host key When check Then return OK")
  void givenMatchingHostKeyWhenCheckThenOk() throws Exception {
    byte[] keyBlob = generatePublicKeyBlob();

    Server server = new Server();
    server.setHostKey(new HostKey("vpn-host", keyBlob).getKey());

    ServerHostKeyRepository repository = new ServerHostKeyRepository(server, serverRepository);

    int result = repository.check("vpn-host", keyBlob);

    assertThat(result).isEqualTo(HostKeyRepository.OK);
  }

  @Test
  @DisplayName("Given different host key When check Then return NOT_INCLUDED")
  void givenDifferentHostKeyWhenCheckThenNotIncluded() throws Exception {
    byte[] storedKeyBlob = generatePublicKeyBlob();
    byte[] incomingKeyBlob = generatePublicKeyBlob();

    Server server = new Server();
    server.setHostKey(new HostKey("vpn-host", storedKeyBlob).getKey());

    ServerHostKeyRepository repository = new ServerHostKeyRepository(server, serverRepository);

    int result = repository.check("vpn-host", incomingKeyBlob);

    assertThat(result).isEqualTo(HostKeyRepository.NOT_INCLUDED);
  }

  @Test
  @DisplayName("Given host key on add/remove When modify Then persist changes")
  void givenAddRemoveWhenModifyThenPersist() throws Exception {
    Server server = new Server();
    server.setHostKey(null);
    ServerHostKeyRepository repository = new ServerHostKeyRepository(server, serverRepository);

    HostKey hostKey = new HostKey("vpn-host", generatePublicKeyBlob());
    repository.add(hostKey, null);

    assertThat(server.getHostKey()).isEqualTo(hostKey.getKey());
    verify(serverRepository, times(1)).save(server);

    repository.remove("vpn-host", "ignored");

    assertThat(server.getHostKey()).isNull();
    verify(serverRepository, times(2)).save(server);
  }

  @Test
  @DisplayName("Given key is null in remove(host,type,byte[]) When remove Then clear host key")
  void givenNullKeyWhenRemoveThenClearHostKey() {
    Server server = new Server();
    server.setHostKey("some-key");

    ServerHostKeyRepository repository = new ServerHostKeyRepository(server, serverRepository);

    repository.remove("vpn-host", "ssh-rsa", null);

    assertThat(server.getHostKey()).isNull();
    verify(serverRepository).save(server);
  }

  @Test
  @DisplayName("Given empty stored host key and non-null key in remove(host,type,byte[]) When remove Then do nothing")
  void givenEmptyHostKeyAndNonNullKeyWhenRemoveThenDoNothing() {
    Server server = new Server();
    server.setHostKey("");

    ServerHostKeyRepository repository = new ServerHostKeyRepository(server, serverRepository);

    repository.remove("vpn-host", "ssh-rsa", new byte[]{1, 2, 3});

    assertThat(server.getHostKey()).isEmpty();
    verify(serverRepository, never()).save(server);
  }

  @Test
  @DisplayName("Given invalid host key string When getHostKey Then return empty array")
  void givenInvalidStoredKeyWhenGetHostKeyThenEmptyArray() {
    Server server = new Server();
    server.setHostKey("invalid-key");
    server.setIp("vpn-host");

    ServerHostKeyRepository repository = new ServerHostKeyRepository(server, serverRepository);

    HostKey[] keys = repository.getHostKey("vpn-host", null);

    assertThat(keys).isEmpty();
  }

  private byte[] generatePublicKeyBlob() throws Exception {
    JSch jsch = new JSch();
    KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, 1024);
    try {
      return keyPair.getPublicKeyBlob();
    } finally {
      keyPair.dispose();
    }
  }
}

