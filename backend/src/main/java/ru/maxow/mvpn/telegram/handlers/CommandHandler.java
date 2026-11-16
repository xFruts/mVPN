package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Интерфейс обработчика команд Telegram-бота.
 */
public interface CommandHandler {

  /**
   * Проверяет, поддерживает ли обработчик данное входящее обновление.
   *
   * @param update входящее обновление Telegram
   * @return {@code true}, если обработчик может обработать обновление,
   *     иначе {@code false}
   */
  boolean supports(Update update);

  /**
   * Обрабатывает входящее обновление и возвращает список действий Telegram API.
   *
   * @param update входящее обновление Telegram
   * @return список объектов {@link BotApiMethod} для отправки
   */
  List<BotApiMethod<?>> handle(Update update);
}
