package ru.maxow.mvpn.telegram.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.maxow.mvpn.device.UserDevice;
import ru.maxow.mvpn.telegram.adapter.TelegramSenderService;
import ru.maxow.mvpn.user.User;

/**
 * Сервис отправки служебных и security-уведомлений пользователям через Telegram.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TelegramNotificationService {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm 'UTC'");

  TelegramSenderService senderService;
  TelegramKeyboardFactory keyboardFactory;
  MessageSource messageSource;

  /**
   * Отправляет пользователю уведомление о подключении нового устройства к VPN.
   *
   * @param user пользователь сервиса
   * @param device данные о новом подключенном устройстве
   */
  public void notifyNewDeviceConnected(User user, UserDevice device) {
    if (user == null || user.getUserTelegramId() == null || device == null) {
      return;
    }
    String chatId = user.getUserTelegramId().toString();
    Locale locale = Locale.forLanguageTag("ru");

    String fallbackModel = messageSource.getMessage(
        "bot.notification.fallback.device", null, locale);
    String rawModel = device.getDeviceModel() != null
        ? device.getDeviceModel()
        : fallbackModel;
    String model = TelegramMessageFormatter.escapeMarkdown(rawModel);

    String fallbackOs = messageSource.getMessage(
        "bot.notification.fallback.os", null, locale);
    String rawOs = device.getOsName() != null
        ? device.getOsName()
        : fallbackOs;
    String os = TelegramMessageFormatter.escapeMarkdown(rawOs);

    String fallbackClient = messageSource.getMessage(
        "bot.notification.fallback.client", null, locale);
    String rawClient = device.getClientApp() != null
        ? device.getClientApp()
        : fallbackClient;
    String client = TelegramMessageFormatter.escapeMarkdown(rawClient);

    String location = TelegramMessageFormatter.escapeMarkdown(
        buildLocationString(device, locale)
    );
    String dateStr = device.getFirstSeenAt() != null
        ? device.getFirstSeenAt().format(DATE_FORMATTER)
        : OffsetDateTime.now().format(DATE_FORMATTER);

    String message = messageSource.getMessage(
        "bot.notification.new-device",
        new Object[] {model, os, client, location, dateStr},
        locale
    );

    InlineKeyboardMarkup keyboard = keyboardFactory.buildNewDeviceNotificationKeyboard(locale);
    senderService.sendMessage(chatId, message.strip(), keyboard);
    log.info("Sent new device notification for device id={}", device.getId());
  }

  private String buildLocationString(UserDevice device, Locale locale) {
    StringBuilder sb = new StringBuilder();
    if (device.getLastIsp() != null && !device.getLastIsp().isBlank()) {
      sb.append(device.getLastIsp());
    }
    if (device.getLastCity() != null && !device.getLastCity().isBlank()) {
      if (!sb.isEmpty()) {
        sb.append(" (").append(device.getLastCity()).append(")");
      } else {
        sb.append(device.getLastCity());
      }
    }
    if (sb.isEmpty()) {
      String fallbackUnknown = messageSource.getMessage(
          "bot.notification.fallback.unknown", null, locale);
      return device.getLastIp() != null ? device.getLastIp() : fallbackUnknown;
    }
    return sb.toString();
  }
}
