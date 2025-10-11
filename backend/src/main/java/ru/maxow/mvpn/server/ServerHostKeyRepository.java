package ru.maxow.mvpn.server;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerHostKeyRepository implements HostKeyRepository {

  Server server;
  ServerRepository serverRepository;


  @Override
  public int check(String host, byte[] key) {
    String hostKey = server.getHostKey();

    if (hostKey == null || hostKey.isEmpty()) {
      return OK;
    }

    try {
      HostKey savedHostKey = new HostKey(host, key);
      String receivedHostKey = savedHostKey.getKey();

      if (hostKey.equals(receivedHostKey)) {
        return OK;
      }
    } catch (JSchException e) {
      return NOT_INCLUDED;
    }

    return NOT_INCLUDED;
  }

  @Override
  public void add(HostKey hostKey, UserInfo userInfo) {
    server.setHostKey(hostKey.getKey());
    serverRepository.save(server);
  }

  @Override
  public void remove(String host, String key) {
    server.setHostKey(null);
    serverRepository.save(server);
  }

  @Override
  public void remove(String host, String type, byte[] key) {
    if (key == null) {
      remove(host, type);
      return;
    }
    String hostKey = server.getHostKey();
    if (hostKey == null || hostKey.isEmpty()) {
      try {
        HostKey savedHostKey = new HostKey(host, key);
        if (hostKey.equals(savedHostKey.getKey())) {
          remove(host, type);
        }
      } catch (JSchException ignored) {
      }
    }
  }

  @Override
  public String getKnownHostsRepositoryID() {
    return "server-" + server.getId();
  }

  @Override
  public HostKey[] getHostKey(String host, String type) {
    String hostKeyString = server.getHostKey();
    if (hostKeyString == null || hostKeyString.isEmpty()) {
      return new HostKey[0];
    }
    try {
      HostKey key = new HostKey(host, HostKey.GUESS, hostKeyString.getBytes());
      return new HostKey[]{key};
    } catch (JSchException e) {
      return new HostKey[0];
    }
  }

  @Override
  public HostKey[] getHostKey() {
    return getHostKey(server.getIp(), null);
  }
}
