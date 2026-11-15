package ru.maxow.mvpn.telegram.model;

import java.util.List;
import ru.maxow.mvpn.device.UserDevice;

/**
 * Сводка по устройствам пользователя (клиенты + IP-сессии).
 *
 * @param activeDevicesCount  число уникальных активных устройств (IP)
 * @param maxDevicesLimit     лимит устройств по тарифу
 * @param remainingSlotsCount число оставшихся доступных слотов
 * @param activeSessions      список активных IP-сессий
 * @param registeredDevices   список распознанных устройств
 */
public record UserDevicesSummary(
    int activeDevicesCount,
    int maxDevicesLimit,
    int remainingSlotsCount,
    List<UserDeviceRecord> activeSessions,
    List<UserDevice> registeredDevices
) {

  public UserDevicesSummary {
    activeSessions = activeSessions == null ? List.of() : List.copyOf(activeSessions);
    registeredDevices = registeredDevices == null ? List.of() : List.copyOf(registeredDevices);
  }

  public UserDevicesSummary(
      int activeDevicesCount,
      int maxDevicesLimit,
      int remainingSlotsCount,
      List<UserDeviceRecord> activeSessions
  ) {
    this(activeDevicesCount, maxDevicesLimit, remainingSlotsCount, activeSessions, List.of());
  }

  // Псевдоним для обратной совместимости
  public List<UserDeviceRecord> devices() {
    return activeSessions;
  }
}
