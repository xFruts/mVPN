package ru.maxow.mvpn.server;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.ServerStatus;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ServerMonitoringServiceImpl implements ServerMonitoringService {

  ServerRepository serverRepository;

  @Override
  @Scheduled(fixedRate = 60000)
  @Transactional
  public void updateServerMetrics() {
    List<Server> servers = serverRepository.findAll();
    log.info("Starting server metrics update for {} servers.", servers.size());

    for (Server server : servers) {
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

        serverRepository.save(server);
        log.info("Updated metrics for server '{}': status={}, ping={}, load={}, uptime={}%",
            server.getName(), server.getStatus(), server.getPing(),
            server.getLoad(), server.getUptime());
      } catch (Exception e) {
        log.error("Failed to update metrics for server {}: {}", server.getName(), e.getMessage());
        server.setStatus(ServerStatus.INACTIVE);
        server.setPing("N/A");
        server.setLoad(0);
        updateUptimePercentage(server, false);
        serverRepository.save(server);
      }
    }
    log.info("Finished server metrics update.");
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
      JSch jsch = new JSch();
      ServerHostKeyRepository hkr = new ServerHostKeyRepository(server, serverRepository);

      jsch.setHostKeyRepository(hkr);
      session = jsch.getSession(server.getLogin(), server.getIp(), 22);
      session.setPassword(server.getPassword());
      session.connect(10000);

      channel = (ChannelExec) session.openChannel("exec");
      channel.setCommand("uptime");
      ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
      channel.setOutputStream(responseStream);
      channel.connect();

      while (channel.isConnected()) {
        Thread.sleep(100);
      }

      String response = responseStream.toString();
      parseUptimeResponse(server, response);
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

  private void parseUptimeResponse(Server server, String response) {
    log.debug("Uptime response for server {}: {}", server.getName(), response);
    try {
      if (response.contains("load average:")) {
        String loadPart = response.substring(
            response.indexOf("load average:") + 14).trim();
        String[] loads = loadPart.split(",\\s*");
        int load = (int) (Double.parseDouble(loads[0].replace(',', '.')) * 100);
        server.setLoad(load);
      }
    } catch (Exception e) {
      log.error("Failed to parse uptime response for server {}: {}",
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
