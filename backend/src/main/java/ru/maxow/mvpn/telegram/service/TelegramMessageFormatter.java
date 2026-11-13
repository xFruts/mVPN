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
        2. Обновите подписку в приложении (Happ, v2rayTun, v2rayNG, Streisand).
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
        🔗 Подключение к mVPN

        Ваша персональная ссылка на подписку:
        %s

        💡 Рекомендуем использовать ссылку на подписку:
        Она объединяет все ваши серверы и автоматически обновляет их без ручной настройки.

        Выберите инструкцию для вашего приложения или получите прямой VLESS-ключ:
        """,
        subscriptionUrl
    ).strip();
  }

  public String formatHappGuide(String subscriptionUrl) {
    return String.format(
        """
        📱 Пошаговая инструкция для Happ (iOS / Android / Mac / Windows)

        Happ — быстрый и удобный клиент с поддержкой VLESS Reality.

        1️⃣ Скопируйте вашу ссылку на подписку:
        %s

        2️⃣ Установите приложение Happ:
        • iOS: найдите «Happ Proxy Utility» в App Store
        • Android: установите из Google Play или GitHub

        3️⃣ Откройте Happ и перейдите во вкладку «Конфигурации» (значок «➕» вверху).

        4️⃣ Нажмите «Добавить подписку» (Add Subscription) и вставьте скопированную ссылку.

        5️⃣ Нажмите иконку 🔄 (Обновить) рядом с mVPN для загрузки списка серверов.

        6️⃣ Выберите нужную страну из списка и нажмите центральную кнопку подключения.

        🎉 Готово! Соединение установлено.
        """,
        subscriptionUrl
    ).strip();
  }

  public String formatV2rayTunGuide(String subscriptionUrl) {
    return String.format(
        """
        📱 Пошаговая инструкция для v2rayTun (Android / iOS)

        v2rayTun — надежный клиент для протокола VLESS Reality.

        Способ 1 — Автообновляемая подписка (Рекомендуется):
        1️⃣ Скопируйте ссылку на подписку:
        %s

        2️⃣ Откройте v2rayTun и нажмите значок «➕» в правом верхнем углу.

        3️⃣ Выберите «Добавить подписку» (Add Subscription URL):
        • Название: mVPN
        • URL подписки: вставьте скопированную ссылку
        Нажмите галочку/кнопку «Сохранить».

        4️⃣ В меню (три точки вверху) выберите «Обновить подписку».

        5️⃣ Нажмите на нужный сервер из списка и запустите подключение кнопкой внизу.

        ───────────────────────
        Способ 2 — Прямой VLESS-ключ:
        1. Получите ключ командой /key или кнопкой «Скопировать VLESS-ключ».
        2. В v2rayTun нажмите «➕» ➔ «Импорт из буфера обмена».
        3. Нажмите кнопку подключения.
        """,
        subscriptionUrl
    ).strip();
  }

  public String formatVlessKey(Optional<String> vlessKeyOpt, String subscriptionUrl) {
    if (vlessKeyOpt.isEmpty()) {
      return String.format(
          """
          ⚠️ Не удалось сформировать прямой VLESS-ключ (сервера временно на обслуживании).

          Пожалуйста, используйте вашу ссылку на подписку:
          %s

          Она автоматически загрузит доступные узлы при восстановлении работы серверов.
          """,
          subscriptionUrl
      ).strip();
    }

    return String.format(
        """
        🔑 Ваш персональный VLESS-ключ:

        %s

        💡 Как использовать:
        1. Скопируйте ключ выше.
        2. Откройте приложение (Happ, v2rayTun, v2rayNG, Streisand, Hiddify).
        3. Нажмите «➕» ➔ «Импорт из буфера обмена» (Import from clipboard).
        4. Выберите добавленный сервер и нажмите «Подключить».
        """,
        vlessKeyOpt.get()
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
          🔗 /connect — Ссылка на подписку и выбор инструкций
          🔑 /key — Получить прямой VLESS-ключ для быстрой вставки
          📱 /happ — Пошаговая инструкция для приложения Happ
          📱 /v2raytun — Пошаговая инструкция для приложения v2rayTun
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
        • Получение ссылок для подключения и ключей VLESS
        • Пошаговые инструкции для Happ и v2rayTun
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
