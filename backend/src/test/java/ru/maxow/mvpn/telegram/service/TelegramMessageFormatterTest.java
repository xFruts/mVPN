package ru.maxow.mvpn.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;

@DisplayName("TelegramMessageFormatter - unit tests")
class TelegramMessageFormatterTest {

  private final TelegramMessageFormatter formatter = new TelegramMessageFormatter();

  @Test
  @DisplayName("formatSubscriptionStatus with unlimited tariff")
  void shouldFormatUnlimitedSubscription() {
    User user = new User();
    Tariff tariff = new Tariff();
    tariff.setName("Unlimited Plan");
    tariff.setTrafficLimitGb(0);

    Subscription sub = new Subscription();
    sub.setTariff(tariff);
    sub.setStartDate(OffsetDateTime.now(ZoneOffset.UTC));
    sub.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(10));

    SubscriptionTrafficState traffic = new SubscriptionTrafficState();
    traffic.setUsedBytes(5L * 1024L * 1024L * 1024L);

    String result = formatter.formatSubscriptionStatus(
        user, Optional.of(sub), Optional.of(traffic));

    assertThat(result).contains("Unlimited Plan");
    assertThat(result).contains("Безлимитный");
    assertThat(result).contains("5.00 ГБ");
  }

  @Test
  @DisplayName("formatSubscriptionStatus with expired subscription")
  void shouldFormatExpiredSubscription() {
    User user = new User();
    Tariff tariff = new Tariff();
    tariff.setName("Basic");
    tariff.setTrafficLimitGb(10);

    Subscription sub = new Subscription();
    sub.setTariff(tariff);
    sub.setStartDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(40));
    sub.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10));

    String result = formatter.formatSubscriptionStatus(
        user, Optional.of(sub), Optional.empty());

    assertThat(result).contains("Истекла");
    assertThat(result).contains("истекла 10 дн. назад");
  }

  @Test
  @DisplayName("formatServersStatus with multiple servers and statuses")
  void shouldFormatServersSafely() {
    Server s1 = new Server();
    s1.setName("Нидерланды");
    s1.setLocation("Amsterdam");
    s1.setCountryEmoji("🇳🇱");
    s1.setStatus(ServerStatus.ACTIVE);
    s1.setIp("1.2.3.4");

    Server s2 = new Server();
    s2.setName("Швеция");
    s2.setCountryEmoji("🇸🇪");
    s2.setStatus(ServerStatus.INACTIVE);
    s2.setIp("5.6.7.8");

    String result = formatter.formatServersStatus(List.of(s1, s2));

    assertThat(result).contains("🇳🇱 Нидерланды (Amsterdam) — 🟢 Работает стабильно");
    assertThat(result).contains("🇸🇪 Швеция — 🔴 Временно недоступен");
    assertThat(result).doesNotContain("1.2.3.4");
    assertThat(result).doesNotContain("5.6.7.8");
  }

  @Test
  @DisplayName("formatServersStatus with empty list")
  void shouldFormatEmptyServers() {
    String result = formatter.formatServersStatus(List.of());
    assertThat(result).contains("список доступных серверов пуст");
  }

  @Test
  @DisplayName("formatHappGuide contains Happ instructions and subscription URL")
  void shouldFormatHappGuide() {
    String url = "https://sub.example.com/v1/config/test-uuid";
    String result = formatter.formatHappGuide(url);

    assertThat(result).contains("Happ");
    assertThat(result).contains(url);
    assertThat(result).contains("Добавить подписку");
  }

  @Test
  @DisplayName("formatV2rayTunGuide contains v2rayTun instructions and subscription URL")
  void shouldFormatV2rayTunGuide() {
    String url = "https://sub.example.com/v1/config/test-uuid";
    String result = formatter.formatV2rayTunGuide(url);

    assertThat(result).contains("v2rayTun");
    assertThat(result).contains(url);
    assertThat(result).contains("Обновить подписку");
  }

  @Test
  @DisplayName("formatVlessKey returns key when present and fallback when empty")
  void shouldFormatVlessKey() {
    String key = "vless://uuid@host:443?type=tcp";
    String url = "https://sub.example.com/v1/config/test-uuid";

    String withKey = formatter.formatVlessKey(Optional.of(key), url);
    assertThat(withKey).contains(key);
    assertThat(withKey).contains("Импорт из буфера обмена");

    String withoutKey = formatter.formatVlessKey(Optional.empty(), url);
    assertThat(withoutKey).contains("Не удалось");
    assertThat(withoutKey).contains(url);
  }
}
