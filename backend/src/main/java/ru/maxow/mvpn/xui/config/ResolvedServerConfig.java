package ru.maxow.mvpn.xui.config;

import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.SubscriptionFormat;

/**
 * Неизменяемый DTO с полученной конфигурацией подключения для сервера.
 */
public record ResolvedServerConfig(
    Server server,
    String config,
    SubscriptionFormat format
) {
  public ResolvedServerConfig {
    if (server == null) {
      throw new IllegalArgumentException("server cannot be null");
    }
    if (config == null || config.isBlank()) {
      throw new IllegalArgumentException("config cannot be null or blank");
    }
    if (format == null) {
      throw new IllegalArgumentException("format cannot be null");
    }
  }
}
