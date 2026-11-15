package ru.maxow.mvpn.device;

import java.util.List;
import java.util.UUID;
import ru.maxow.mvpn.user.User;

/**
 * Интерфейс сервиса учета подключенных пользовательских устройств.
 */
public interface UserDeviceService {

  default DeviceRecordResult recordDeviceAccess(
      User user,
      String userAgent,
      String clientIp
  ) {
    return recordDeviceAccess(user, userAgent, clientIp, null);
  }

  DeviceRecordResult recordDeviceAccess(
      User user,
      String userAgent,
      String clientIp,
      String hwid
  );

  List<UserDevice> getUserDevices(User user);

  void clearUserDevices(User user);

  /**
   * Безопасно регистрирует обращение устройства по verificationCode пользователя
   * и отправляет Telegram-уведомление при обнаружении нового устройства.
   */
  void recordAccessSafely(
      UUID verificationCode,
      String userAgent,
      String clientIp,
      String hwid
  );
}
