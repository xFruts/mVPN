package ru.maxow.mvpn.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.user.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDeviceServiceImpl - unit tests")
class UserDeviceServiceImplTest {

  @Mock
  private UserDeviceRepository userDeviceRepository;

  @Mock
  private GeoIpService geoIpService;

  @Mock
  private ru.maxow.mvpn.user.UserRepository userRepository;

  @Mock
  private ru.maxow.mvpn.telegram.service.TelegramNotificationService notificationService;

  private final UserAgentDeviceParser parser = new UserAgentDeviceParser();
  private UserDeviceServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new UserDeviceServiceImpl(
        userDeviceRepository,
        parser,
        geoIpService,
        userRepository,
        notificationService
    );
  }

  @Test
  @DisplayName("Given new device When recordDeviceAccess Then save and return isNewDevice=true")
  void givenNewDeviceWhenRecordAccessThenSaveNew() {
    User user = new User();
    user.setId(42L);

    when(geoIpService.lookup("198.51.100.1"))
        .thenReturn(Optional.of(
            new GeoIpInfo("198.51.100.1", "Ростелеком", "Москва", "RU", "RU")
        ));
    when(userDeviceRepository.findByUserIdAndDeviceFingerprint(eq(42L), any()))
        .thenReturn(Optional.empty());
    when(userDeviceRepository.findByUserIdOrderByLastSeenAtDesc(42L))
        .thenReturn(List.of());
    when(userDeviceRepository.save(any(UserDevice.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DeviceRecordResult result = service.recordDeviceAccess(
        user,
        "Happ/4.5.4/Windows/2610141139502/MaxowPC_x86_64",
        "198.51.100.1"
    );

    assertThat(result.isNewDevice()).isTrue();
    assertThat(result.device()).isNotNull();
    assertThat(result.device().getClientApp()).isEqualTo("Happ 4.5.4");
    assertThat(result.device().getDeviceModel()).isEqualTo("Windows 11 (24H2)");
    assertThat(result.device().getLastIsp()).isEqualTo("Ростелеком");
    assertThat(result.device().getLastCity()).isEqualTo("Москва");
  }

  @Test
  @DisplayName("Given existing device When recordDeviceAccess Then update and return isNewDevice=false")
  void givenExistingDeviceWhenRecordAccessThenUpdateExisting() {
    User user = new User();
    user.setId(42L);

    UserDevice existing = new UserDevice();
    existing.setId(99L);
    existing.setUser(user);
    existing.setFirstSeenAt(OffsetDateTime.now().minusDays(3));

    when(geoIpService.lookup("198.51.100.1")).thenReturn(Optional.empty());
    when(userDeviceRepository.findByUserIdAndDeviceFingerprint(eq(42L), any()))
        .thenReturn(Optional.of(existing));
    when(userDeviceRepository.save(any(UserDevice.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DeviceRecordResult result = service.recordDeviceAccess(
        user,
        "Happ/4.5.4/Windows/2610141139502/MaxowPC_x86_64",
        "198.51.100.1"
    );

    assertThat(result.isNewDevice()).isFalse();
    assertThat(result.device().getId()).isEqualTo(99L);
  }

  @Test
  @DisplayName("Given same IP with client switched When recordDeviceAccess Then match existing device")
  void givenClientSwitchOnSameIpWhenRecordAccessThenMatchExistingAndNotNew() {
    User user = new User();
    user.setId(42L);

    UserDevice existing = new UserDevice();
    existing.setId(77L);
    existing.setUser(user);
    existing.setLastIp("198.51.100.1");
    existing.setOsName("Windows 11 (24H2)");
    existing.setDeviceModel("MaxowPC_x86_64");
    existing.setClientApp("Happ 4.5.4");

    when(userDeviceRepository.findByUserIdAndDeviceFingerprint(eq(42L), any()))
        .thenReturn(Optional.empty());
    when(userDeviceRepository.findByUserIdOrderByLastSeenAtDesc(42L))
        .thenReturn(List.of(existing));
    when(userDeviceRepository.save(any(UserDevice.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    // User opens v2rayTun on the same Windows PC and same home IP
    DeviceRecordResult result = service.recordDeviceAccess(
        user,
        "v2rayTun/1.5.0 (Windows NT 10.0; Win64; x64)",
        "198.51.100.1"
    );

    assertThat(result.isNewDevice()).isFalse();
    assertThat(result.device().getId()).isEqualTo(77L);
    assertThat(result.device().getClientApp()).isEqualTo("v2rayTun 1.5.0");
    assertThat(result.device().getDeviceModel()).isEqualTo("MaxowPC_x86_64");
  }

  @Test
  @DisplayName("Given mobile to WiFi network switch When recordDeviceAccess Then update ISP without alert")
  void givenNetworkSwitchFromMobileToWifiWhenRecordAccessThenUpdateIspAndNotNew() {
    User user = new User();
    user.setId(42L);

    UserDevice existing = new UserDevice();
    existing.setId(88L);
    existing.setUser(user);
    existing.setLastIp("94.25.1.1");
    existing.setLastIsp("МегаФон");
    existing.setLastCity("Санкт-Петербург");
    existing.setDeviceModel("iPhone 16 Pro");
    existing.setOsName("iOS 18.1");
    existing.setClientApp("Happ 4.5.4");

    when(geoIpService.lookup("178.62.2.2"))
        .thenReturn(Optional.of(
            new GeoIpInfo("178.62.2.2", "Ростелеком", "Санкт-Петербург", "RU", "RU")
        ));
    when(userDeviceRepository.findByUserIdAndDeviceFingerprint(eq(42L), any()))
        .thenReturn(Optional.of(existing));
    when(userDeviceRepository.save(any(UserDevice.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    // Connects from Home Wi-Fi
    DeviceRecordResult result = service.recordDeviceAccess(
        user,
        "Happ/4.5.4/iOS/iPhone17,1/18.1",
        "178.62.2.2"
    );

    assertThat(result.isNewDevice()).isFalse();
    assertThat(result.device().getId()).isEqualTo(88L);
    assertThat(result.device().getLastIp()).isEqualTo("178.62.2.2");
    assertThat(result.device().getLastIsp()).isEqualTo("Ростелеком");
  }

  @Test
  @DisplayName("Given user When clearUserDevices Then call repository deleteByUserId")
  void givenUserWhenClearDevicesThenDeleteByUserId() {
    User user = new User();
    user.setId(42L);

    service.clearUserDevices(user);

    verify(userDeviceRepository).deleteByUserId(42L);
  }

  @Test
  @DisplayName("Given single mobile device on LTE with dynamic IP Then match and not new device")
  void givenSingleMobileDeviceOnLteWithDynamicIpWhenRecordAccessThenMatchExisting() {
    User user = new User();
    user.setId(42L);

    UserDevice existing = new UserDevice();
    existing.setId(55L);
    existing.setUser(user);
    existing.setLastIp("10.0.0.1");
    existing.setOsName("Android 14");
    existing.setDeviceModel("Android Устройство");
    existing.setClientApp("v2rayTun 1.5.0");

    when(userDeviceRepository.findByUserIdAndDeviceFingerprint(eq(42L), any()))
        .thenReturn(Optional.empty());
    when(userDeviceRepository.findByUserIdOrderByLastSeenAtDesc(42L))
        .thenReturn(List.of(existing));
    when(userDeviceRepository.save(any(UserDevice.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    // Mobile operator changed IP to 10.0.0.2
    DeviceRecordResult result = service.recordDeviceAccess(
        user,
        "v2rayTun/1.5.0 (Android 14)",
        "10.0.0.2"
    );

    assertThat(result.isNewDevice()).isFalse();
    assertThat(result.device().getId()).isEqualTo(55L);
    assertThat(result.device().getLastIp()).isEqualTo("10.0.0.2");
  }

  @Test
  @DisplayName("Given browser preview link access When recordDeviceAccess Then isNewDevice is false")
  void givenBrowserPreviewAccessWhenRecordAccessThenIsNewDeviceIsFalse() {
    User user = new User();
    user.setId(42L);

    when(userDeviceRepository.findByUserIdAndDeviceFingerprint(eq(42L), any()))
        .thenReturn(Optional.empty());
    when(userDeviceRepository.findByUserIdOrderByLastSeenAtDesc(42L))
        .thenReturn(List.of());
    when(userDeviceRepository.save(any(UserDevice.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DeviceRecordResult result = service.recordDeviceAccess(
        user,
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/130.0.0.0",
        "198.51.100.1"
    );

    assertThat(result.isNewDevice()).isFalse();
    assertThat(result.device()).isNotNull();
    assertThat(result.device().getClientApp()).isEqualTo("Google Chrome");
  }

  @Test
  @DisplayName("Given X-Hwid provided When recordDeviceAccess Then use HWID in fingerprint")
  void givenHwidWhenRecordAccessThenUseHwidFingerprint() {
    User user = new User();
    user.setId(42L);

    when(userDeviceRepository.findByUserIdAndDeviceFingerprint(eq(42L), eq("hwid:test-hwid-123")))
        .thenReturn(Optional.empty());
    when(userDeviceRepository.findByUserIdOrderByLastSeenAtDesc(42L))
        .thenReturn(List.of());
    when(userDeviceRepository.save(any(UserDevice.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    DeviceRecordResult result = service.recordDeviceAccess(
        user,
        "Happ/4.5.4/iOS/iPhone17,1/18.1",
        "198.51.100.1",
        "test-hwid-123"
    );

    assertThat(result.isNewDevice()).isTrue();
    assertThat(result.device().getDeviceFingerprint()).isEqualTo("hwid:test-hwid-123");
  }
}
