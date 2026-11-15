package ru.maxow.mvpn.device;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("UserAgentDeviceParser - unit tests")
class UserAgentDeviceParserTest {

  private UserAgentDeviceParser parser;

  @BeforeEach
  void setUp() {
    parser = new UserAgentDeviceParser();
  }

  @Test
  @DisplayName("Given Happ on Windows When parse Then extract Windows version")
  void givenHappOnWindowsWhenParseThenExtractDetails() {
    String ua = "Happ/4.5.4/Windows/2610141139502/MaxowPC_x86_64";
    ParsedDeviceInfo info = parser.parse(ua);

    assertThat(info.clientApp()).isEqualTo("Happ 4.5.4");
    assertThat(info.osName()).isEqualTo("Windows 11 (24H2)");
    assertThat(info.deviceModel()).isEqualTo("Windows 11 (24H2)");
  }

  @Test
  @DisplayName("Given Happ on iOS When parse Then extract iOS version")
  void givenHappOnIosWhenParseThenMapIphone() {
    String ua = "Happ/4.5.4/iOS/iPhone15,2/17.5";
    ParsedDeviceInfo info = parser.parse(ua);

    assertThat(info.clientApp()).isEqualTo("Happ 4.5.4");
    assertThat(info.osName()).isEqualTo("iOS 17.5");
    assertThat(info.deviceModel()).isEqualTo("iOS 17.5");
  }

  @Test
  @DisplayName("Given Happ on Android When parse Then extract Android version")
  void givenHappOnAndroidWhenParseThenMapSamsung() {
    String ua = "Happ/4.5.4/Android/14/SM-S918B";
    ParsedDeviceInfo info = parser.parse(ua);

    assertThat(info.clientApp()).isEqualTo("Happ 4.5.4");
    assertThat(info.osName()).isEqualTo("Android 14");
    assertThat(info.deviceModel()).isEqualTo("Android 14");
  }

  @Test
  @DisplayName("Given v2rayTun on Android When parse Then extract Android OS version")
  void givenV2rayTunWhenParseThenExtractDetails() {
    String ua = "v2rayTun/1.5.0 (Android 14; Pixel 8)";
    ParsedDeviceInfo info = parser.parse(ua);

    assertThat(info.clientApp()).isEqualTo("v2rayTun 1.5.0");
    assertThat(info.osName()).isEqualTo("Android 14");
    assertThat(info.deviceModel()).isEqualTo("Android 14");
  }

  @Test
  @DisplayName("Given Streisand on iOS When parse Then extract iOS version")
  void givenStreisandWhenParseThenMapIphone() {
    String ua = "Streisand/1.6.2 (iOS 17.4; iPhone14,5)";
    ParsedDeviceInfo info = parser.parse(ua);

    assertThat(info.clientApp()).isEqualTo("Streisand 1.6.2");
    assertThat(info.osName()).isEqualTo("iOS 17.4");
    assertThat(info.deviceModel()).isEqualTo("iOS 17.4");
  }

  @Test
  @DisplayName("Given Chrome on Windows When parse Then detect browser and OS")
  void givenChromeWhenParseThenDetectBrowser() {
    String ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0";
    ParsedDeviceInfo info = parser.parse(ua);

    assertThat(info.clientApp()).isEqualTo("Google Chrome");
    assertThat(info.osName()).isEqualTo("Windows");
    assertThat(info.deviceModel()).isEqualTo("Windows");
  }

  @Test
  @DisplayName("Given null or empty When parse Then return fallback gracefully")
  void givenNullOrEmptyWhenParseThenReturnFallback() {
    ParsedDeviceInfo nullInfo = parser.parse(null);
    assertThat(nullInfo.clientApp()).isEqualTo("VPN Client");
    assertThat(nullInfo.osName()).isEqualTo("Unknown");

    ParsedDeviceInfo emptyInfo = parser.parse("   ");
    assertThat(emptyInfo.clientApp()).isEqualTo("VPN Client");
    assertThat(emptyInfo.osName()).isEqualTo("Unknown");
  }

  @Test
  @DisplayName("Given sing-box When parse Then detect client")
  void givenSingBoxWhenParseThenDetectClient() {
    ParsedDeviceInfo info = parser.parse("sing-box/1.9.0 (Windows NT 10.0; Win64; x64)");
    assertThat(info.clientApp()).isEqualTo("sing-box 1.9.0");
    assertThat(info.osName()).isEqualTo("Windows");
    assertThat(info.deviceModel()).isEqualTo("Windows");
  }

  @Test
  @DisplayName("Given browser or crawler User-Agents When isBrowserOrCrawler Then return true")
  void givenBrowserOrCrawlerWhenIsBrowserOrCrawlerThenTrue() {
    assertThat(parser.isBrowserOrCrawler("Google Chrome", "Mozilla/5.0 Chrome/130")).isTrue();
    assertThat(parser.isBrowserOrCrawler("Safari", "Mozilla/5.0 Safari/604")).isTrue();
    assertThat(parser.isBrowserOrCrawler("VPN Client", "TelegramBot (like TwitterBot)")).isTrue();
    assertThat(parser.isBrowserOrCrawler("Happ 4.5.4", "Happ/4.5.4")).isFalse();
    assertThat(parser.isBrowserOrCrawler("v2rayTun 1.5.0", "v2rayTun/1.5.0")).isFalse();
  }

  @Test
  @DisplayName("Given client apps with versions When extractClientFamily Then return family name")
  void givenClientAppsWhenExtractClientFamilyThenReturnFamily() {
    assertThat(parser.extractClientFamily("Happ 4.5.4")).isEqualTo("happ");
    assertThat(parser.extractClientFamily("Happ 5.0.0")).isEqualTo("happ");
    assertThat(parser.extractClientFamily("v2rayTun 1.5.0")).isEqualTo("v2ray");
    assertThat(parser.extractClientFamily("Streisand 1.6.2")).isEqualTo("streisand");
    assertThat(parser.extractClientFamily("Google Chrome")).isEqualTo("chrome");
  }
}
