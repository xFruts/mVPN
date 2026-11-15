package ru.maxow.mvpn.telegram.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import ru.maxow.mvpn.device.UserDevice;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.telegram.model.UserDeviceRecord;
import ru.maxow.mvpn.telegram.model.UserDevicesSummary;

/**
 * Форматирует текстовые сообщения и локализованные шаблоны для Telegram-бота.
 */
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TelegramMessageFormatter {

  private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("ru");
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm 'UTC'");

  MessageSource messageSource;

  /**
   * Конструктор с внедрением источника сообщений.
   *
   * @param messageSource источник локализованных сообщений
   */
  @Autowired
  public TelegramMessageFormatter(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Конструктор по умолчанию с базовым ResourceBundleMessageSource.
   */
  public TelegramMessageFormatter() {
    ResourceBundleMessageSource bundleSource = new ResourceBundleMessageSource();
    bundleSource.setBasenames("messages");
    bundleSource.setDefaultEncoding("UTF-8");
    bundleSource.setUseCodeAsDefaultMessage(true);
    bundleSource.setFallbackToSystemLocale(false);
    this.messageSource = bundleSource;
  }

  public String formatUnknownCommand() {
    return formatUnknownCommand(DEFAULT_LOCALE);
  }

  public String formatUnknownCommand(Locale locale) {
    return msg("bot.command.unknown", locale).strip();
  }

  public String formatUnauthorized() {
    return formatUnauthorized(DEFAULT_LOCALE);
  }

  public String formatUnauthorized(Locale locale) {
    return msg("bot.auth.required", locale).strip();
  }

  public String formatNoActiveSubscription(Locale locale) {
    return msg("bot.subscription.none", locale).strip();
  }

  public String formatAuthAlready() {
    return formatAuthAlready(DEFAULT_LOCALE);
  }

  public String formatAuthAlready(Locale locale) {
    return msg("bot.auth.already", locale).strip();
  }

  public String formatAuthSuccess() {
    return formatAuthSuccess(DEFAULT_LOCALE);
  }

  public String formatAuthSuccess(Locale locale) {
    return msg("bot.auth.success", locale).strip();
  }

  public String formatAuthInvalidToken() {
    return formatAuthInvalidToken(DEFAULT_LOCALE);
  }

  public String formatAuthInvalidToken(Locale locale) {
    return msg("bot.auth.invalid-token", locale).strip();
  }

  public String formatAuthInvalidFormat() {
    return formatAuthInvalidFormat(DEFAULT_LOCALE);
  }

  public String formatAuthInvalidFormat(Locale locale) {
    return msg("bot.auth.invalid-format", locale).strip();
  }

  public String formatAuthTokenRequired() {
    return formatAuthTokenRequired(DEFAULT_LOCALE);
  }

  public String formatAuthTokenRequired(Locale locale) {
    return msg("bot.auth.token-required", locale).strip();
  }

  public String formatSubscriptionStatus(
      Subscription subscription,
      SubscriptionTrafficState traffic
  ) {
    return formatSubscriptionStatus(subscription, traffic, DEFAULT_LOCALE);
  }

  /**
   * Форматирует подробную информацию о статусе подписки пользователя.
   *
   * @param subscription сущность подписки
   * @param traffic состояние использованного трафика
   * @param locale локаль пользователя
   * @return отформатированное сообщение о подписке
   */
  public String formatSubscriptionStatus(
      Subscription subscription,
      SubscriptionTrafficState traffic,
      Locale locale
  ) {
    if (subscription == null) {
      return msg("bot.subscription.info.none", locale).strip();
    }

    Tariff tariff = subscription.getTariff();

    String tariffName = (tariff != null && tariff.getName() != null)
        ? escapeMarkdown(tariff.getName())
        : msg("bot.subscription.tariff.unspecified", locale);

    String statusBadge = subscription.isActive()
        ? msg("bot.subscription.status.active", locale)
        : msg("bot.subscription.status.inactive", locale);

    String durationNote = calculateDurationNote(subscription.getEndDate(), locale);
    String formattedDate = subscription.getEndDate() != null
        ? subscription.getEndDate().format(DATE_FORMATTER)
        : msg("bot.subscription.date.permanent", locale);

    String maxDevices = (tariff != null && tariff.getMaxDevices() != null)
        ? String.valueOf(tariff.getMaxDevices())
        : "1";

    String trafficSection;
    if (traffic != null) {
      long limitBytes = tariff != null ? tariff.getTrafficLimitBytes() : 0L;
      trafficSection = formatTrafficSection(traffic.getEffectiveUsedBytes(), limitBytes, locale);
    } else {
      trafficSection = msg("bot.subscription.traffic.unavailable", locale);
    }

    return msg(
        "bot.subscription.info.details",
        locale,
        tariffName,
        statusBadge,
        formattedDate,
        durationNote,
        maxDevices,
        trafficSection
    ).strip();
  }

  /**
   * Форматирует сводку по зарегистрированным и активным устройствам пользователя.
   *
   * @param summary сводная информация об устройствах
   * @param locale локаль пользователя
   * @return отформатированный текст со списком устройств
   */
  public String formatDevices(UserDevicesSummary summary, Locale locale) {
    if (summary == null) {
      return msg("bot.devices.none", locale).strip();
    }

    StringBuilder sb = new StringBuilder();
    sb.append(msg(
        "bot.devices.header",
        locale,
        summary.maxDevicesLimit(),
        summary.activeDevicesCount(),
        summary.remainingSlotsCount()
    ));

    boolean hasRegistered = summary.registeredDevices() != null
        && !summary.registeredDevices().isEmpty();
    boolean hasSessions = summary.activeSessions() != null
        && !summary.activeSessions().isEmpty();

    if (!hasRegistered && !hasSessions) {
      sb.append(msg("bot.devices.empty", locale));
      return sb.toString().strip();
    }

    if (hasRegistered) {
      sb.append(msg("bot.devices.registered.header", locale));
      int idx = 1;
      for (UserDevice dev : summary.registeredDevices()) {
        String model = dev.getDeviceModel() != null && !dev.getDeviceModel().isBlank()
            && !"unknown".equalsIgnoreCase(dev.getDeviceModel())
            ? escapeMarkdown(dev.getDeviceModel())
            : msg("bot.device.default-name", locale);
        String os = dev.getOsName() != null && !dev.getOsName().isBlank()
            && !"unknown".equalsIgnoreCase(dev.getOsName())
            ? escapeMarkdown(dev.getOsName())
            : msg("bot.os.default-name", locale);
        String isp = dev.getLastIsp() != null
            ? escapeMarkdown(dev.getLastIsp())
            : "—";
        if (dev.getLastCity() != null && !dev.getLastCity().isBlank()) {
          isp += " (" + escapeMarkdown(dev.getLastCity()) + ")";
        }
        String date = dev.getFirstSeenAt() != null
            ? dev.getFirstSeenAt().format(DATE_FORMATTER)
            : "—";
        sb.append(msg("bot.devices.registered.item", locale, idx++, model, os, isp, date));
      }
      sb.append("\n");
    }

    if (hasSessions) {
      sb.append(msg("bot.devices.list.header", locale));
      for (UserDeviceRecord device : summary.activeSessions()) {
        String servers = "";
        if (device.serverNames() != null && !device.serverNames().isEmpty()) {
          List<String> escapedServers = device.serverNames().stream()
              .map(TelegramMessageFormatter::escapeMarkdown)
              .toList();
          servers = String.join(", ", escapedServers);
        }
        String info = servers;
        if (device.ispLocation() != null && !device.ispLocation().isBlank()) {
          String escapedIsp = escapeMarkdown(device.ispLocation());
          info = !info.isBlank()
              ? servers + " (" + escapedIsp + ")"
              : escapedIsp;
        }
        if (!info.isBlank()) {
          sb.append(msg("bot.devices.item", locale, escapeMarkdown(device.ip()), info));
        } else {
          sb.append(msg("bot.devices.item.no-server", locale, escapeMarkdown(device.ip())));
        }
      }
      sb.append(msg("bot.devices.footer", locale));
    }

    return sb.toString().strip();
  }

  public String formatServersStatus(List<Server> servers) {
    return formatServersStatus(servers, DEFAULT_LOCALE);
  }

  /**
   * Форматирует список доступных VPN-серверов со статусами их доступности.
   *
   * @param servers список серверов
   * @param locale локаль пользователя
   * @return отформатированный текст списка серверов
   */
  public String formatServersStatus(List<Server> servers, Locale locale) {
    if (servers == null || servers.isEmpty()) {
      return msg("bot.servers.empty", locale).strip();
    }

    StringBuilder sb = new StringBuilder(msg("bot.servers.header", locale));
    for (Server server : servers) {
      String emoji = server.getCountryEmoji() != null ? server.getCountryEmoji() : "🌐";
      String name = server.getName() != null
          ? escapeMarkdown(server.getName())
          : msg("bot.server.default-name", locale);
      String location = server.getLocation() != null && !server.getLocation().isBlank()
          ? " (" + escapeMarkdown(server.getLocation()) + ")"
          : "";
      String statusBadge = formatServerBadge(server.getStatus(), locale);
      sb.append(emoji)
          .append(' ')
          .append(name)
          .append(location)
          .append(" — ")
          .append(statusBadge)
          .append('\n');
    }

    sb.append(msg("bot.servers.footer", locale));
    return sb.toString().strip();
  }

  public String formatConnectionGuide(String subscriptionUrl, Locale locale) {
    return msg("bot.connection.guide", locale, subscriptionUrl).strip();
  }

  public String formatHappGuide(String subscriptionUrl) {
    return formatHappGuide(subscriptionUrl, DEFAULT_LOCALE);
  }

  public String formatHappGuide(String subscriptionUrl, Locale locale) {
    return msg("bot.guide.happ", locale, subscriptionUrl).strip();
  }

  public String formatV2rayTunGuide(String subscriptionUrl) {
    return formatV2rayTunGuide(subscriptionUrl, DEFAULT_LOCALE);
  }

  public String formatV2rayTunGuide(String subscriptionUrl, Locale locale) {
    return msg("bot.guide.v2raytun", locale, subscriptionUrl).strip();
  }

  public String formatVlessKey(String vlessKey, String subscriptionUrl) {
    return formatVlessKey(vlessKey, subscriptionUrl, DEFAULT_LOCALE);
  }

  /**
   * Форматирует сообщение с VLESS-ключом подключения.
   *
   * @param vlessKey ключ подключения формата VLESS
   * @param subscriptionUrl ссылка на общую подписку
   * @param locale локаль пользователя
   * @return отформатированный текст с ключом
   */
  public String formatVlessKey(
      String vlessKey,
      String subscriptionUrl,
      Locale locale
  ) {
    if (vlessKey != null && !vlessKey.isBlank()) {
      return msg("bot.vless.key", locale, vlessKey).strip();
    }
    return msg("bot.vless.unavailable", locale, subscriptionUrl).strip();
  }

  public String formatWelcome(String firstName, boolean isAuthenticated) {
    return formatWelcome(firstName, isAuthenticated, DEFAULT_LOCALE);
  }

  /**
   * Форматирует приветственное сообщение при запуске бота.
   *
   * @param firstName имя пользователя
   * @param isAuthenticated признак авторизации пользователя
   * @param locale локаль пользователя
   * @return приветственный текст
   */
  public String formatWelcome(String firstName, boolean isAuthenticated, Locale locale) {
    String name = (firstName != null && !firstName.isBlank())
        ? escapeMarkdown(firstName)
        : msg("bot.welcome.default-name", locale);
    if (isAuthenticated) {
      return msg("bot.welcome.authenticated", locale, name).strip();
    }
    return msg("bot.welcome.unauthenticated", locale, name).strip();
  }

  /**
   * Форматирует справочное сообщение со списком доступных команд.
   *
   * @param isAuthenticated признак авторизации пользователя
   * @param locale локаль пользователя
   * @return текст справки
   */
  public String formatHelp(boolean isAuthenticated, Locale locale) {
    if (isAuthenticated) {
      return msg("bot.help.authenticated", locale).strip();
    }
    return msg("bot.help.unauthenticated", locale).strip();
  }

  /**
   * Экранирует специальные символы Markdown для Telegram.
   *
   * @param text исходный текст
   * @return экранированный текст
   */
  public static String escapeMarkdown(String text) {
    if (text == null) {
      return "";
    }
    return text
        .replace("_", "\\_")
        .replace("*", "\\*")
        .replace("`", "\\`")
        .replace("[", "\\[");
  }

  private String msg(String code, Locale locale, Object... args) {
    Locale target = locale != null ? locale : DEFAULT_LOCALE;
    return messageSource.getMessage(code, args, target);
  }

  private String formatTrafficSection(long usedBytes, long limitBytes, Locale locale) {
    if (limitBytes <= 0) {
      return msg("bot.subscription.traffic.unlimited", locale, formatBytes(usedBytes, locale));
    }

    long remainingBytes = Math.max(0, limitBytes - usedBytes);
    int percentRemaining = (int) Math.clamp(
        Math.round(((double) remainingBytes / limitBytes) * 100),
        0,
        100
    );
    String progressBar = buildProgressBar(percentRemaining);

    return msg(
        "bot.subscription.traffic.limited",
        locale,
        formatBytes(usedBytes, locale),
        formatBytes(limitBytes, locale),
        formatBytes(remainingBytes, locale),
        percentRemaining,
        progressBar
    );
  }

  private String buildProgressBar(int percent) {
    int totalBars = 10;
    int filledBars = Math.clamp((int) Math.round(percent / 10.0), 0, totalBars);
    return "▰".repeat(filledBars) + "▱".repeat(totalBars - filledBars);
  }

  private String formatBytes(long bytes, Locale locale) {
    if (bytes < 1024 * 1024) {
      return msg("bot.unit.kb", locale, bytes / 1024);
    }
    if (bytes >= 1024L * 1024L * 1024L) {
      String formatted = String.format(Locale.US, "%.2f", bytes / (1024.0 * 1024.0 * 1024.0));
      return msg("bot.unit.gb", locale, formatted);
    }
    String formatted = String.format(Locale.US, "%.1f", bytes / (1024.0 * 1024.0));
    return msg("bot.unit.mb", locale, formatted);
  }

  private String calculateDurationNote(OffsetDateTime endDate, Locale locale) {
    if (endDate == null) {
      return "";
    }
    OffsetDateTime now = OffsetDateTime.now();
    if (endDate.isBefore(now)) {
      long daysAgo = Math.max(1, Duration.between(endDate, now).toDays());
      return msg("bot.subscription.duration.expired", locale, daysAgo);
    }
    long daysLeft = Math.max(0, Duration.between(now, endDate).toDays());
    return msg("bot.subscription.duration.remaining", locale, daysLeft);
  }

  private String formatServerBadge(ServerStatus status, Locale locale) {
    if (status == null) {
      return msg("bot.server.status.active", locale);
    }
    return switch (status) {
      case ACTIVE -> msg("bot.server.status.active", locale);
      case MAINTENANCE -> msg("bot.server.status.maintenance", locale);
      case INACTIVE -> msg("bot.server.status.inactive", locale);
    };
  }
}
