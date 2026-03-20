package ru.maxow.mvpn.telegram.adapter;

import java.util.List;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.telegram.handlers.CommandHandler;

/**
 * Dispatches incoming Telegram updates to the appropriate command handler.
 */
@Component
public class UpdateDispatcher {
  private final List<CommandHandler> handlers;

  /**
   * Constructs an UpdateDispatcher with the given list of command handlers.
   *
   * @param handlers the list of command handlers
   */
  public UpdateDispatcher(List<CommandHandler> handlers) {
    this.handlers = List.copyOf(handlers);
  }

  /**
   * Dispatches the given update to the appropriate command handler.
   *
   * @param update the incoming Telegram update
   * @return a list of SendMessage responses from the handler,
   *     or an empty list if no handler supports the update
   */
  public List<SendMessage> dispatch(Update update) {
    return handlers.stream()
        .filter(h -> h.supports(update))
        .findFirst()
        .map(h -> h.handle(update))
        .orElse(List.of());
  }
}
