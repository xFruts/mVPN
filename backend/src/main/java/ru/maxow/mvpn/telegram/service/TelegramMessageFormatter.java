package ru.maxow.mvpn.telegram.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;

@Component
public class TelegramMessageFormatter {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm 'UTC'");

  public String formatSubscriptionStatus(
      User user,
      Optional<Subscription> subscriptionOpt,
      Optional<SubscriptionTrafficState> trafficOpt
  ) {
    if (subscriptionOpt.isEmpty()) {
      return """
          ℹ️ Информация о подписке

          У вас пока нет активной подписки.
          Оформить подписку можно в личном кабинете mVPN.
          """;
    }

    Subscription sub = subscriptionOpt.get();
    Tariff tariff = sub.getTariff();
    String tariffName = tariff != null ? tariff.getName() : "Не указан";
    boolean isActive = sub.isActive();
    String statusBadge = isActive ? "🟢 Активна" : "🔴 Истекла / Неактивна";

    OffsetDateTime endDate = sub.getEndDate();
    String formattedDate = endDate != null ? endDate.format(DATE_FORMATTER) : "Бессрочно";
    String durationNote = calculateDurationNote(endDate);

    long limitBytes = sub.getTrafficLimitBytes();
    long usedBytes = trafficOpt
        .map(SubscriptionTrafficState::getEffectiveUsedBytes)
        .orElse(0L);

    String trafficInfo = formatTrafficSection(usedBytes, limitBytes);
    int maxDevices = tariff != null && tariff.getMaxDevices() != null
        ? tariff.getMaxDevices()
        : 1;

    return String.format(
        """
        📊 Ваша подписка:

        ⭐️ Тариф: %s
        Статус: %s
        🗓 Действует до: %s %s
        📱 Устройств: до %d

        %s
        """,
        tariffName,
        statusBadge,
        formattedDate,
        durationNote,
        maxDevices,
        trafficInfo
    ).strip();
  }

  public String formatServersStatus(List<Server> servers) {
    if (servers == null || servers.isEmpty()) {
      return "🌐 В данный момент список доступных серверов пуст.";
    }

    StringBuilder sb = new StringBuilder("🌐 Доступные сервера и локации:\n\n");
    for (Server server : servers) {
      String emoji = server.getCountryEmoji() != null ? server.getCountryEmoji() : "🌐";
      String name = server.getName() != null ? server.getName() : "Сервер";
      String location = server.getLocation() != null && !server.getLocation().isBlank()
          ? " (" + server.getLocation() + ")"
          : "";
      String statusBadge = formatServerBadge(server.getStatus());

      sb.append(String.format("%s %s%s — %s\n", emoji, name, location, statusBadge));
    }

    sb.append("""

        ───────────────────────
        💡 Если сервер со статусом 🟢 у вас не работает:
        1. Проверьте подключение к сети без VPN.
        2. Обновите подписку в приложении (v2rayNG, Streisand, Happ, Hiddify).
        3. Если локация недоступна, переключитесь на другую рабочую страну.
        """);

    return sb.toString().strip();
  }

  public String formatConnectionGuide(
      User user,
      Optional<Subscription> subscriptionOpt,
      String subscriptionUrl
  ) {
    if (subscriptionOpt.isEmpty() || !subscriptionOpt.get().isActive()) {
      return """
          ⚠️ У вас нет активной подписки.

          Для получения ссылки на подключение оформите подписку в личном кабинете.
          """;
    }

    return String.format(
        """
        🔗 Подключение к VPN

        Ваша персональная ссылка на подписку:
        `%s`

        📲 Как подключить:
        1. Скопируйте ссылку выше.
        2. Откройте приложение:
           • Android: v2rayNG / Hiddify / Happ
           • iOS: Streisand / V2Box / Happ
           • Windows / Mac: Hiddify / Nekoray
        3. Нажмите «+» или «Добавить подписку» и вставьте ссылку.
        4. Обновите подписку и выберите любую локацию!
        """,
        subscriptionUrl
    ).strip();
  }

  public String formatWelcome(String firstName, boolean isAuthenticated) {
    String name = firstName != null && !firstName.isBlank() ? firstName : "друг";
    if (isAuthenticated) {
      return String.format(
          """
          👋 С возвращением, %s!

          Вы авторизованы в сервисе mVPN.
          Используйте кнопки меню ниже для управления доступом.
          """,
          name
      ).strip();
    }
    return String.format(
        """
        👋 Привет, %s!
        Я официальный бот VPN-сервиса mVPN.

        Для привязки аккаунта введите команду:
        `/auth <код_авторизации>`
        (код можно скопировать в личном кабинете на сайте)

        Справка по командам: /help
        """,
        name
    ).strip();
  }

  public String formatHelp(boolean isAuthenticated) {
    if (isAuthenticated) {
      return """
          ℹ️ Доступные команды mVPN бота:

          📊 /status — Информация о тарифе, сроке действия и остатке ГБ
          🌐 /servers — Список доступных стран и статус их работы
          🔗 /connect — Ссылка на подписку для приложений
          ❓ /help — Показать эту справку

          Вы также можете пользоваться кнопками меню на клавиатуре.
          """;
    }
    return """
        ℹ️ Справка по боту:

        Для начала работы необходимо привязать Telegram к вашему аккаунту mVPN:
        `/auth <ваш_токен>`

        После успешной привязки вам станут доступны:
        • Просмотр тарифа и остатка ГБ
        • Проверка доступности серверов
        • Получение ссылок для подключения
        """;
  }

  private String formatTrafficSection(long usedBytes, long limitBytes) {
    if (limitBytes <= 0) {
      return String.format(
          "📈 Трафик:\nИспользовано: %s\nЛимит: Безлимитный",
          formatBytes(usedBytes)
      );
    }

    long remainingBytes = Math.max(0, limitBytes - usedBytes);
    int percentRemaining = (int) Math.min(
        100,
        Math.round((double) remainingBytes / limitBytes * 100)
    );
    String bar = renderProgressBar(remainingBytes, limitBytes);

    return String.format(
        "📈 Трафик:\nИспользовано: %s из %s\n⏳ Осталось: %s (%d%%)\n%s",
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

  private String calculateDurationNote(OffsetDateTime endDate) {
    if (endDate == null) {
      return "";
    }
    OffsetDateTime now = OffsetDateTime.now();
    if (endDate.isBefore(now)) {
      long pastDays = Duration.between(endDate, now).toDays();
      return String.format("(истекла %d дн. назад)", Math.max(1, pastDays));
    }
    long remainingDays = Duration.between(now, endDate).toDays();
    return String.format("(осталось %d дн.)", remainingDays);
  }

  private String formatServerBadge(ServerStatus status) {
    if (status == ServerStatus.ACTIVE) {
      return "🟢 Работает стабильно";
    }
    if (status == ServerStatus.MAINTENANCE) {
      return "🟡 Технические работы";
    }
    return "🔴 Временно недоступен";
  }
}
