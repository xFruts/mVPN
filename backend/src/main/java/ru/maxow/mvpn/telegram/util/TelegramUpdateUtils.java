package ru.maxow.mvpn.telegram.util;

import lombok.experimental.UtilityClass;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Утилитные методы для извлечения данных из {@link Update}.
 */
@UtilityClass
public class TelegramUpdateUtils {

  /**
   * Извлекает username пользователя из сообщения или callback.
   *
   * @param update входящее обновление Telegram
   * @return username пользователя или {@code null}
   */
  public static String extractUsername(Update update) {
    if (update == null) {
      return null;
    }
    if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
      return update.getCallbackQuery().getFrom().getUserName();
    }
    if (update.hasMessage() && update.getMessage().getFrom() != null) {
      return update.getMessage().getFrom().getUserName();
    }
    return null;
  }

  /**
   * Извлекает chatId из сообщения или callback query.
   *
   * @param update входящее обновление Telegram
   * @return идентификатор чата или {@code null}
   */
  public static Long extractChatId(Update update) {
    if (update == null) {
      return null;
    }
    if (update.hasCallbackQuery() && update.getCallbackQuery().getMessage() != null) {
      return update.getCallbackQuery().getMessage().getChatId();
    }
    if (update.hasMessage()) {
      return update.getMessage().getChatId();
    }
    return null;
  }
}
