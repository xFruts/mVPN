package ru.maxow.mvpn.telegram.model;

import java.util.List;

/**
 * Запись об активном подключенном устройстве (IP-сессии на серверах).
 *
 * @param ip            IP-адрес устройства
 * @param serverNames   список серверов, где зафиксирован IP (с эмодзи флагов)
 * @param ispLocation   провайдер и геолокация IP (например, "Ростелеком (Москва)")
 * @param lastTimestamp метка времени последней активности
 */
public record UserDeviceRecord(
    String ip,
    List<String> serverNames,
    String ispLocation,
    Long lastTimestamp
) {

  public UserDeviceRecord {
    serverNames = serverNames == null ? List.of() : List.copyOf(serverNames);
  }

  public UserDeviceRecord(String ip, List<String> serverNames, Long lastTimestamp) {
    this(ip, serverNames, null, lastTimestamp);
  }
}
