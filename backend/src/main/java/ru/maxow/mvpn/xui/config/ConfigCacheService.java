package ru.maxow.mvpn.xui.config;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConfigCacheService {
  private static class Entry {
    final ru.maxow.mvpn.xui.SubscriptionConfigPayload payload;
    final Instant created;

    Entry(ru.maxow.mvpn.xui.SubscriptionConfigPayload payload) {
      this.payload = payload;
      this.created = Instant.now();
    }
  }

  private final ConcurrentHashMap<Long, Entry> cache = new ConcurrentHashMap<>();
  // default TTL 16 hours
  private static final long TTL_SECONDS = 16L * 60L * 60L;

  public ru.maxow.mvpn.xui.SubscriptionConfigPayload get(Long userId) {
    Entry e = cache.get(userId);
    if (e == null) return null;
    if (Instant.now().isAfter(e.created.plusSeconds(TTL_SECONDS))) {
      cache.remove(userId);
      return null;
    }
    return e.payload;
  }

  public void put(Long userId, ru.maxow.mvpn.xui.SubscriptionConfigPayload payload) {
    evict(userId);
    cache.put(userId, new Entry(payload));
  }

  public void evict(Long userId) {
    cache.remove(userId);
  }
}

