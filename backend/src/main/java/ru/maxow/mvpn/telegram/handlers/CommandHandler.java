package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
   * Handles the given update and returns a list of messages to be sent.
   *
   * @param update the incoming update
   * @return a list of SendMessage objects to be sent as responses
   */
  List<SendMessage> handle(Update update);
}
