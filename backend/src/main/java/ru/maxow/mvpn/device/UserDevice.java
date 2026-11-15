package ru.maxow.mvpn.device;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.user.User;

/**
 * Сущность подключенного устройства пользователя.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_devices")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserDevice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  User user;

  @Column(name = "device_fingerprint", nullable = false, length = 128)
  String deviceFingerprint;

  @Column(name = "client_app", length = 100)
  String clientApp;

  @Column(name = "device_model", length = 150)
  String deviceModel;

  @Column(name = "os_name", length = 100)
  String osName;

  @Column(name = "last_ip", length = 45)
  String lastIp;

  @Column(name = "last_isp", length = 150)
  String lastIsp;

  @Column(name = "last_city", length = 100)
  String lastCity;

  @Column(name = "first_seen_at", nullable = false)
  OffsetDateTime firstSeenAt;

  @Column(name = "last_seen_at", nullable = false)
  OffsetDateTime lastSeenAt;

  @Column(name = "is_active", nullable = false)
  boolean active = true;

  /**
   * Фабричный метод для создания новой записи устройства.
   */
  public static UserDevice create(
      User user,
      String fingerprint,
      String clientApp,
      String deviceModel,
      String osName,
      String clientIp,
      String isp,
      String city,
      OffsetDateTime now
  ) {
    UserDevice device = new UserDevice();
    device.setUser(user);
    device.setDeviceFingerprint(fingerprint);
    device.setClientApp(clientApp);
    device.setDeviceModel(deviceModel);
    device.setOsName(osName);
    device.setLastIp(clientIp);
    device.setLastIsp(isp);
    device.setLastCity(city);
    device.setFirstSeenAt(now);
    device.setLastSeenAt(now);
    device.setActive(true);
    return device;
  }

  /**
   * Обновляет данные последнего подключения устройства.
   */
  public void updateAccess(
      String clientIp,
      String isp,
      String city,
      OffsetDateTime now
  ) {
    this.lastSeenAt = now;
    this.lastIp = clientIp;
    if (isp != null && !isp.isBlank()) {
      this.lastIsp = isp;
    }
    if (city != null && !city.isBlank()) {
      this.lastCity = city;
    }
    this.active = true;
  }
}
