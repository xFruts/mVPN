package ru.maxow.mvpn.telegram.adapter;

import java.util.List;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.telegram.handlers.CommandHandler;

/**
 * Диспетчер входящих обновлений Telegram для маршрутизации
 * в соответствующие обработчики команд.
 */
@Component
public class UpdateDispatcher {
  private final List<CommandHandler> handlers;

  /**
   * Конструктор диспетчера обновлений со списком обработчиков команд.
   *
   * @param handlers список обработчиков команд
   */
  public UpdateDispatcher(List<CommandHandler> handlers) {
    this.handlers = List.copyOf(handlers);
  }

  /**
   * Направляет входящее обновление в соответствующий обработчик команды.
   *
   * @param update входящее обновление Telegram
   * @return список методов Telegram Bot API для отправки пользователю,
   *     или пустой список, если обработчик не найден
   */
  public List<BotApiMethod<?>> dispatch(Update update) {
    return handlers.stream()
        .filter(h -> h.supports(update))
        .findFirst()
        .map(h -> h.handle(update))
        .orElse(List.of());
  }
}
