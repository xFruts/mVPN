package ru.maxow.mvpn.device;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.telegram.service.TelegramNotificationService;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;

/**
 * Сервис управления и учета устройств пользователей.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserDeviceServiceImpl implements UserDeviceService {

  UserDeviceRepository userDeviceRepository;
  UserAgentDeviceParser parser;
  GeoIpService geoIpService;
  UserRepository userRepository;
  TelegramNotificationService notificationService;

  @Override
  @Transactional
  public void recordAccessSafely(
      UUID verificationCode,
      String userAgent,
      String clientIp,
      String hwid
  ) {
    if (verificationCode == null) {
      return;
    }
    userRepository.findByVerificationCode(verificationCode).ifPresent(user -> {
      DeviceRecordResult result = recordDeviceAccess(user, userAgent, clientIp, hwid);
      if (result.isNewDevice()) {
        notificationService.notifyNewDeviceConnected(user, result.device());
      }
    });
  }

  @Override
  @Transactional
  public DeviceRecordResult recordDeviceAccess(
      User user,
      String userAgent,
      String clientIp
  ) {
    return recordDeviceAccess(user, userAgent, clientIp, null);
  }

  @Override
  @Transactional
  public DeviceRecordResult recordDeviceAccess(
      User user,
      String userAgent,
      String clientIp,
      String hwid
  ) {
    if (user == null) {
      return new DeviceRecordResult(null, false);
    }

    ParsedDeviceInfo parsed = parser.parse(userAgent);
    String fingerprint = computeFingerprint(parsed, hwid);

    Optional<GeoIpInfo> geoOpt = geoIpService.lookup(clientIp);
    String isp = geoOpt.map(GeoIpInfo::isp).orElse(null);
    String city = geoOpt.map(GeoIpInfo::city).orElse(null);

    OffsetDateTime now = OffsetDateTime.now();
    Optional<UserDevice> existingOpt = userDeviceRepository
        .findByUserIdAndDeviceFingerprint(user.getId(), fingerprint);

    if (existingOpt.isPresent()) {
      UserDevice device = existingOpt.get();
      return updateDevice(device, clientIp, isp, city, now);
    }

    List<UserDevice> userDevices = userDeviceRepository
        .findByUserIdOrderByLastSeenAtDesc(user.getId());
    Optional<UserDevice> matchedOpt = findMatchingDevice(userDevices, parsed, clientIp);

    if (matchedOpt.isPresent()) {
      UserDevice device = matchedOpt.get();
      if (!fingerprint.equals(device.getDeviceFingerprint())) {
        Optional<UserDevice> clash = userDeviceRepository
            .findByUserIdAndDeviceFingerprint(user.getId(), fingerprint);
        if (clash.isEmpty()) {
          device.setDeviceFingerprint(fingerprint);
        }
      }
      String oldApp = device.getClientApp();
      if (parsed.clientApp() != null && !parsed.clientApp().equals(oldApp)) {
        log.debug("Client app switched on existing device id={}: {} -> {}",
            device.getId(), oldApp, parsed.clientApp());
        device.setClientApp(parsed.clientApp());
      }
      if (isMoreSpecific(parsed.deviceModel(), device.getDeviceModel())) {
        device.setDeviceModel(parsed.deviceModel());
      }
      if (isMoreSpecific(parsed.osName(), device.getOsName())) {
        device.setOsName(parsed.osName());
      }
      return updateDevice(device, clientIp, isp, city, now);
    }

    UserDevice newDevice = UserDevice.create(
        user,
        fingerprint,
        parsed.clientApp(),
        parsed.deviceModel(),
        parsed.osName(),
        clientIp,
        isp,
        city,
        now
    );

    UserDevice saved = userDeviceRepository.save(newDevice);
    boolean isBrowser = parser.isBrowserOrCrawler(parsed.clientApp(), userAgent);
    boolean isNewDevice = !isBrowser;
    log.info("Registered device: user={}, model={}, client={}, ip={}, isNewDevice={}",
        user.getId(), parsed.deviceModel(), parsed.clientApp(),
        GeoIpService.maskIp(clientIp), isNewDevice);
    return new DeviceRecordResult(saved, isNewDevice);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserDevice> getUserDevices(User user) {
    if (user == null || user.getId() == null) {
      return List.of();
    }
    return userDeviceRepository.findByUserIdOrderByLastSeenAtDesc(user.getId());
  }

  @Override
  @Transactional
  public void clearUserDevices(User user) {
    if (user != null && user.getId() != null) {
      userDeviceRepository.deleteByUserId(user.getId());
      log.info("Cleared registered devices for user id={}", user.getId());
    }
  }

  private DeviceRecordResult updateDevice(
      UserDevice device,
      String clientIp,
      String isp,
      String city,
      OffsetDateTime now
  ) {
    device.updateAccess(clientIp, isp, city, now);
    userDeviceRepository.save(device);
    log.debug("Updated existing device: id={}, user={}",
        device.getId(), device.getUser().getId());
    return new DeviceRecordResult(device, false);
  }

  private Optional<UserDevice> findMatchingDevice(
      List<UserDevice> devices,
      ParsedDeviceInfo parsed,
      String clientIp
  ) {
    if (devices == null || devices.isEmpty()) {
      return Optional.empty();
    }
    OsFamily candidateOs = OsFamily.from(parsed.osName());

    for (UserDevice dev : devices) {
      OsFamily devOs = OsFamily.from(dev.getOsName());
      if (devOs != candidateOs || devOs == OsFamily.UNKNOWN) {
        continue;
      }

      boolean devSpecific = isSpecific(dev.getDeviceModel());
      boolean candSpecific = isSpecific(parsed.deviceModel());

      // 1. Точное совпадение модели на совместимой ОС
      if (devSpecific && candSpecific) {
        if (dev.getDeviceModel().equalsIgnoreCase(parsed.deviceModel())) {
          return Optional.of(dev);
        }
        // Разные конкретные модели на одной ОС — это разные устройства
        continue;
      }

      // 2. Тот же IP и то же семейство ОС
      if (clientIp != null && !clientIp.isBlank()
          && clientIp.equals(dev.getLastIp())) {
        return Optional.of(dev);
      }

      // 3. Единственное устройство пользователя на этой ОС (смена IP Wi-Fi/моб. сеть)
      long sameOsCount = devices.stream()
          .filter(d -> OsFamily.from(d.getOsName()) == candidateOs)
          .count();
      if (sameOsCount == 1) {
        return Optional.of(dev);
      }

      // 4. Существующее устройство имело общее имя (браузерный предпросмотр) -> обновляем
      if (!devSpecific) {
        return Optional.of(dev);
      }
    }
    return Optional.empty();
  }

  private boolean isSpecific(String model) {
    if (model == null || model.isBlank()) {
      return false;
    }
    String clean = model.trim().toLowerCase();
    if ("unknown".equals(clean)
        || clean.startsWith("windows")
        || clean.startsWith("ios")
        || clean.startsWith("android")
        || clean.startsWith("macos")
        || clean.startsWith("linux")
        || clean.contains("устройство")
        || clean.contains("компьютер")
        || clean.contains("device")) {
      return false;
    }
    return true;
  }

  private boolean isMoreSpecific(String candidate, String current) {
    return isSpecific(candidate) && !isSpecific(current);
  }

  /**
   * Вычисляет уникальный фингерпринт устройства.
   * При наличии HWID (например, от клиента Happ) используется он с префиксом.
   * В остальных случаях формируется SHA-256 хэш от комбинации
   * семейства клиента, ОС и модели устройства.
   */
  private String computeFingerprint(ParsedDeviceInfo parsed, String hwid) {
    if (hwid != null && !hwid.isBlank()) {
      String clean = hwid.trim().toLowerCase();
      if (clean.length() > 64) {
        clean = clean.substring(0, 64);
      }
      return "hwid:" + clean;
    }
    String clientFamily = parser.extractClientFamily(parsed.clientApp());
    String osFamily = OsFamily.from(parsed.osName()).name().toLowerCase();
    String model = parsed.deviceModel() != null ? parsed.deviceModel().trim().toLowerCase() : "";
    String source = clientFamily + "|" + osFamily + "|" + model;
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] hash = md.digest(source.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      return Integer.toHexString(source.hashCode());
    }
  }

  private enum OsFamily {
    WINDOWS,
    IOS,
    ANDROID,
    MACOS,
    LINUX,
    UNKNOWN;

    static OsFamily from(String os) {
      if (os == null || os.isBlank()) {
        return UNKNOWN;
      }
      String s = os.toLowerCase();
      if (s.contains("windows")) {
        return WINDOWS;
      }
      if (s.contains("ios")) {
        return IOS;
      }
      if (s.contains("android")) {
        return ANDROID;
      }
      if (s.contains("macos")) {
        return MACOS;
      }
      if (s.contains("linux")) {
        return LINUX;
      }
      return UNKNOWN;
    }
  }
}
