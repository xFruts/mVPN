package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Interface for handling commands in a Telegram bot.
 */
public interface CommandHandler {
  /**
   * Checks if the handler supports the given update.
   *
   * @param update the incoming update
   * @return true if the handler can process the update, false otherwise
   */
  boolean supports(Update update);

  /**
   * Handles the given update and returns a list of BotApiMethod actions to be executed.
   *
   * @param update the incoming update
   * @return a list of BotApiMethod objects (e.g. SendMessage, EditMessageText) to execute
   */
  List<BotApiMethod<?>> handle(Update update);
}
