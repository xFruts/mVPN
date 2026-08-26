package ru.maxow.mvpn.server;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.model.ServerStatus;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.InetAddress;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ServerMonitoringServiceImpl implements ServerMonitoringService {

  ServerRepository serverRepository;
  ServerSshKeyStorageService sshKeyStorageService;

  @Override
  @Scheduled(fixedRate = 60000)
  public void updateServersMetrics() {
    List<Server> servers = serverRepository.findAll();
    log.debug("Starting server metrics update for {} servers.", servers.size());

    for (Server server : servers) {
      processSingleServer(server);
    }

    log.debug("Finished server metrics update.");
  }

  private void processSingleServer(Server server) {
    try {
      boolean isReachable = updatePing(server);

      if (isReachable) {
        updateSshMetrics(server);
        if (server.getStatus() == ServerStatus.INACTIVE) {
          server.setStatus(ServerStatus.ACTIVE);
        }
      } else {
        server.setStatus(ServerStatus.INACTIVE);
        server.setLoad(0);
      }

      updateUptimePercentage(server, isReachable);

      saveServerSafely(server);

      log.debug("Updated metrics for server '{}': status={}, ping={}, load={}, uptime={}%",
          server.getName(), server.getStatus(), server.getPing(),
          server.getLoad(), server.getUptime());

    } catch (ObjectOptimisticLockingFailureException e) {
      log.warn("Server '{}' (id={}) was deleted during metrics update, skipping.",
          server.getName(), server.getId());
    } catch (Exception e) {
      log.error("Failed to update metrics for server {}: {}", server.getName(), e.getMessage());
      try {
        server.setStatus(ServerStatus.INACTIVE);
        server.setPing("N/A");
        server.setLoad(0);
        updateUptimePercentage(server, false);
        saveServerSafely(server);
      } catch (Exception ex) {
        log.warn("Could not save inactive status for server '{}': {}", server.getName(), ex.getMessage());
      }
    }
  }

  private void saveServerSafely(Server server) {
    if (serverRepository.existsById(server.getId())) {
      serverRepository.save(server);
    } else {
      log.warn("Server '{}' (id={}) no longer exists in database, skipping save.",
          server.getName(), server.getId());
    }
  }

  private boolean updatePing(Server server) throws Exception {
    long startTime = System.currentTimeMillis();
    boolean reachable = InetAddress.getByName(server.getIp())
        .isReachable(5000); // 5 сек таймаут
    long endTime = System.currentTimeMillis();

    if (reachable) {
      server.setPing((endTime - startTime) + " ms");
    } else {
      server.setPing("N/A");
    }
    return reachable;
  }

  private void updateSshMetrics(Server server) {
    Session session = null;
    ChannelExec channel = null;
    try {
      validateSshAuthConfiguration(server);

      JSch jsch = new JSch();
      ServerHostKeyRepository hkr = new ServerHostKeyRepository(server, serverRepository);

      jsch.setHostKeyRepository(hkr);

      if (isKeyAuth(server)) {
        byte[] privateKey = sshKeyStorageService.downloadPrivateKey(server.getSshPrivateKeyObjectKey());
        jsch.addIdentity("server-" + server.getId(), privateKey, null, null);
      }

      session = jsch.getSession(server.getLogin(), server.getIp(), 22);

      if (!isKeyAuth(server)) {
        session.setPassword(server.getPassword());
      }

      session.connect(10000);

      channel = (ChannelExec) session.openChannel("exec");
      // loadavg 1-min value + CPU count → load as 0–100% of capacity
      channel.setCommand("cat /proc/loadavg; nproc");
      try (InputStream responseStream = channel.getInputStream()) {
        channel.connect();

        String response = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
        parseLoadResponse(server, response);
      }
    } catch (Exception e) {
      log.error("SSH error for server {}: {}", server.getName(), e.getMessage());
      server.setStatus(ServerStatus.INACTIVE);
      server.setLoad(0);
      throw new RuntimeException("Failed to execute SSH command", e);
    } finally {
      if (channel != null && channel.isConnected()) {
        channel.disconnect();
      }
      if (session != null && session.isConnected()) {
        session.disconnect();
      }
    }
  }

  private boolean isKeyAuth(Server server) {
    return SshAuthType.KEY.equals(resolveAuthType(server));
  }

  private SshAuthType resolveAuthType(Server server) {
    return server.getSshAuthType() == null ? SshAuthType.PASSWORD : server.getSshAuthType();
  }

  private void validateSshAuthConfiguration(Server server) {
    SshAuthType authType = resolveAuthType(server);

    if (SshAuthType.KEY.equals(authType)
        && (server.getSshPrivateKeyObjectKey() == null || server.getSshPrivateKeyObjectKey().isBlank())) {
      throw new IllegalArgumentException(
          "SSH key auth requires sshPrivateKeyObjectKey for server '" + server.getName() + "'.");
    }

    if (SshAuthType.PASSWORD.equals(authType)
        && (server.getPassword() == null || server.getPassword().isBlank())) {
      throw new IllegalArgumentException(
          "SSH password auth requires password for server '" + server.getName() + "'.");
    }
  }

  /**
   * Expects output of {@code cat /proc/loadavg; nproc}, e.g.:
   * <pre>
   * 1.86 1.50 1.20 1/234 5678
   * 4
   * </pre>
   * Load is stored as percent of CPU capacity: {@code round(load1m / nproc * 100)}, clamped to 0–100.
   * Also accepts legacy {@code uptime} text with {@code load average:} (assumes 1 CPU).
   */
  void parseLoadResponse(Server server, String response) {
    log.debug("Load response for server {}: {}", server.getName(), response);
    try {
      if (response == null || response.isBlank()) {
        server.setLoad(0);
        return;
      }

      String trimmed = response.trim();
      double loadAvg;
      int cpus = 1;

      if (trimmed.contains("load average:") || trimmed.contains("load averages:")) {
        String marker = trimmed.contains("load average:") ? "load average:" : "load averages:";
        String loadPart = trimmed.substring(trimmed.indexOf(marker) + marker.length()).trim();
        // Locale uptime uses ", " between values and ',' as decimal (e.g. "0,42, 0,30, 0,20")
        String firstLoad = loadPart.contains(", ")
            ? loadPart.split(", ", 2)[0].trim()
            : loadPart.split("\\s+")[0].trim();
        loadAvg = Double.parseDouble(firstLoad.replace(',', '.'));
      } else {
        String[] lines = trimmed.split("\\R");
        String[] loadFields = lines[0].trim().split("\\s+");
        loadAvg = Double.parseDouble(loadFields[0].replace(',', '.'));
        if (lines.length > 1) {
          cpus = Integer.parseInt(lines[lines.length - 1].trim());
        }
      }

      if (cpus < 1) {
        cpus = 1;
      }

      int loadPercent = (int) Math.round((loadAvg / cpus) * 100.0);
      server.setLoad(Math.clamp(loadPercent, 0, 100));
    } catch (Exception e) {
      log.error("Failed to parse load response for server {}: {}",
          server.getName(), e.getMessage());
      server.setLoad(0);
    }
  }

  private void updateUptimePercentage(Server server, boolean isSuccessfulCheck) {
    long successfulChecks = server.getSuccessfulChecks() != null ? server.getSuccessfulChecks() : 0;
    long failedChecks = server.getFailedChecks() != null ? server.getFailedChecks() : 0;

    if (isSuccessfulCheck) {
      successfulChecks++;
    } else {
      failedChecks++;
    }

    server.setSuccessfulChecks(successfulChecks);
    server.setFailedChecks(failedChecks);

    long totalChecks = successfulChecks + failedChecks;
    if (totalChecks > 0) {
      double uptime = (successfulChecks * 100.0) / totalChecks;
      server.setUptime(Math.round(uptime * 100.0) / 100.0); // round to 2 decimal places
    } else {
      server.setUptime(100.0);
    }
  }
}
