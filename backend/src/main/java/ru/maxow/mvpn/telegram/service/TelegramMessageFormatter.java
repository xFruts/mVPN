package ru.maxow.mvpn.telegram.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TelegramMessageFormatter {

  private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("ru");
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm 'UTC'");

  MessageSource messageSource;

  @Autowired
  public TelegramMessageFormatter(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public TelegramMessageFormatter() {
    ResourceBundleMessageSource bundleSource = new ResourceBundleMessageSource();
    bundleSource.setBasenames("messages");
    bundleSource.setDefaultEncoding("UTF-8");
    bundleSource.setUseCodeAsDefaultMessage(true);
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

  public String formatNoActiveSubscription() {
    return formatNoActiveSubscription(DEFAULT_LOCALE);
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
      User user,
      Optional<Subscription> subscriptionOpt,
      Optional<SubscriptionTrafficState> trafficOpt
  ) {
    return formatSubscriptionStatus(user, subscriptionOpt, trafficOpt, DEFAULT_LOCALE);
  }

  public String formatSubscriptionStatus(
      User user,
      Optional<Subscription> subscriptionOpt,
      Optional<SubscriptionTrafficState> trafficOpt,
      Locale locale
  ) {
    if (subscriptionOpt.isEmpty()) {
      return msg("bot.subscription.info.none", locale).strip();
    }

    Subscription sub = subscriptionOpt.get();
    Tariff tariff = sub.getTariff();
    String tariffName = tariff != null
        ? tariff.getName()
        : msg("bot.subscription.tariff.unspecified", locale);
    boolean isActive = sub.isActive();
    String statusBadge = isActive
        ? msg("bot.subscription.status.active", locale)
        : msg("bot.subscription.status.inactive", locale);

    OffsetDateTime endDate = sub.getEndDate();
    String formattedDate = endDate != null
        ? endDate.format(DATE_FORMATTER)
        : msg("bot.subscription.date.permanent", locale);
    String durationNote = calculateDurationNote(endDate, locale);

    long limitBytes = sub.getTrafficLimitBytes();
    long usedBytes = trafficOpt
        .map(SubscriptionTrafficState::getEffectiveUsedBytes)
        .orElse(0L);

    String trafficInfo = formatTrafficSection(usedBytes, limitBytes, locale);
    int maxDevices = tariff != null && tariff.getMaxDevices() != null
        ? tariff.getMaxDevices()
        : 1;

    return msg(
        "bot.subscription.info.details",
        locale,
        tariffName,
        statusBadge,
        formattedDate,
        durationNote,
        maxDevices,
        trafficInfo
    ).strip();
  }

  public String formatServersStatus(List<Server> servers) {
    return formatServersStatus(servers, DEFAULT_LOCALE);
  }

  public String formatServersStatus(List<Server> servers, Locale locale) {
    if (servers == null || servers.isEmpty()) {
      return msg("bot.servers.empty", locale).strip();
    }

    StringBuilder sb = new StringBuilder(msg("bot.servers.header", locale));
    for (Server server : servers) {
      String emoji = server.getCountryEmoji() != null ? server.getCountryEmoji() : "🌐";
      String name = server.getName() != null ? server.getName() : "Сервер";
      String location = server.getLocation() != null && !server.getLocation().isBlank()
          ? " (" + server.getLocation() + ")"
          : "";
      String statusBadge = formatServerBadge(server.getStatus(), locale);

      sb.append(String.format("%s %s%s — %s\n", emoji, name, location, statusBadge));
    }

    sb.append(msg("bot.servers.footer", locale));
    return sb.toString().strip();
  }

  public String formatConnectionGuide(
      User user,
      Optional<Subscription> subscriptionOpt,
      String subscriptionUrl
  ) {
    return formatConnectionGuide(user, subscriptionOpt, subscriptionUrl, DEFAULT_LOCALE);
  }

  public String formatConnectionGuide(
      User user,
      Optional<Subscription> subscriptionOpt,
      String subscriptionUrl,
      Locale locale
  ) {
    if (subscriptionOpt.isEmpty() || !subscriptionOpt.get().isActive()) {
      return msg("bot.connection.no-subscription", locale).strip();
    }

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

  public String formatVlessKey(Optional<String> vlessKeyOpt, String subscriptionUrl) {
    return formatVlessKey(vlessKeyOpt, subscriptionUrl, DEFAULT_LOCALE);
  }

  public String formatVlessKey(
      Optional<String> vlessKeyOpt,
      String subscriptionUrl,
      Locale locale
  ) {
    if (vlessKeyOpt.isEmpty()) {
      return msg("bot.vless.unavailable", locale, subscriptionUrl).strip();
    }

    return msg("bot.vless.key", locale, vlessKeyOpt.get()).strip();
  }

  public String formatWelcome(String firstName, boolean isAuthenticated) {
    return formatWelcome(firstName, isAuthenticated, DEFAULT_LOCALE);
  }

  public String formatWelcome(String firstName, boolean isAuthenticated, Locale locale) {
    String defaultName = (locale != null && locale.getLanguage().equalsIgnoreCase("en"))
        ? "friend"
        : "друг";
    String name = firstName != null && !firstName.isBlank() ? firstName : defaultName;
    if (isAuthenticated) {
      return msg("bot.welcome.authenticated", locale, name).strip();
    }
    return msg("bot.welcome.unauthenticated", locale, name).strip();
  }

  public String formatHelp(boolean isAuthenticated) {
    return formatHelp(isAuthenticated, DEFAULT_LOCALE);
  }

  public String formatHelp(boolean isAuthenticated, Locale locale) {
    if (isAuthenticated) {
      return msg("bot.help.authenticated", locale).strip();
    }
    return msg("bot.help.unauthenticated", locale).strip();
  }

  private String msg(String code, Locale locale, Object... args) {
    Locale target = locale != null ? locale : DEFAULT_LOCALE;
    return messageSource.getMessage(code, args, target);
  }

  private String formatTrafficSection(long usedBytes, long limitBytes, Locale locale) {
    if (limitBytes <= 0) {
      return msg("bot.subscription.traffic.unlimited", locale, formatBytes(usedBytes));
    }

    long remainingBytes = Math.max(0, limitBytes - usedBytes);
    int percentRemaining = (int) Math.min(
        100,
        Math.round((double) remainingBytes / limitBytes * 100)
    );
    String bar = renderProgressBar(remainingBytes, limitBytes);

    return msg(
        "bot.subscription.traffic.limited",
        locale,
        formatBytes(usedBytes),
        formatBytes(limitBytes),
        formatBytes(remainingBytes),
        percentRemaining,
        bar
    );
  }

  private String renderProgressBar(long remainingBytes, long totalBytes) {
    if (totalBytes <= 0) {
      return "[██████████] 100%";
    }
    int totalBlocks = 10;
    int filledBlocks = (int) Math.min(
        totalBlocks,
        Math.max(0, Math.round((double) remainingBytes / totalBytes * totalBlocks))
    );
    int emptyBlocks = totalBlocks - filledBlocks;
    int percent = (int) Math.min(100, Math.round((double) remainingBytes / totalBytes * 100));

    return "[" + "█".repeat(filledBlocks) + "░".repeat(emptyBlocks) + "] " + percent + "%";
  }

  private String formatBytes(long bytes) {
    if (bytes >= 1024L * 1024L * 1024L) {
      return String.format(Locale.US, "%.2f ГБ", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    return String.format(Locale.US, "%.1f МБ", bytes / (1024.0 * 1024.0));
  }

  private String calculateDurationNote(OffsetDateTime endDate, Locale locale) {
    if (endDate == null) {
      return "";
    }
    OffsetDateTime now = OffsetDateTime.now();
    if (endDate.isBefore(now)) {
      long pastDays = Duration.between(endDate, now).toDays();
      return msg("bot.subscription.duration.expired", locale, Math.max(1, pastDays));
    }
    long remainingDays = Duration.between(now, endDate).toDays();
    return msg("bot.subscription.duration.remaining", locale, remainingDays);
  }

  private String formatServerBadge(ServerStatus status, Locale locale) {
    if (status == ServerStatus.ACTIVE) {
      return msg("bot.server.status.active", locale);
    }
    if (status == ServerStatus.MAINTENANCE) {
      return msg("bot.server.status.maintenance", locale);
    }
    return msg("bot.server.status.inactive", locale);
  }
}
