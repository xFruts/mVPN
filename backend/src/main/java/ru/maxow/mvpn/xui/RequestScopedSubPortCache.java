package ru.maxow.mvpn.xui;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequestScope
public class RequestScopedSubPortCache {
  private final Map<Long, Integer> subPortByServerId = new ConcurrentHashMap<>();

  public Integer get(Long serverId) {
    return serverId == null ? null : subPortByServerId.get(serverId);
  }

  public void put(Long serverId, int subPort) {
    if (serverId != null) {
      subPortByServerId.put(serverId, subPort);
    }
  }
}

