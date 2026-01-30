package ru.maxow.mvpn.telegram.adapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * VPNBot is a Telegram bot that handles incoming updates and dispatches them for processing.
 * It extends TelegramLongPollingBot to receive updates via long polling.
 */
@Component
@ConditionalOnProperty(prefix = "bot", name = "enabled", havingValue = "true")
public class VpnBot extends TelegramLongPollingBot {

  private final UpdateDispatcher dispatcher;
  private final String username;

  /**
   * Constructs a new VpnBot instance.
   *
   * @param botToken the token for the bot, injected from application properties
   * @param username the username of the bot, injected from application properties
   * @param dispatcher the UpdateDispatcher to handle incoming updates
   * @param telegramSenderService the service to send messages via Telegram
   */
  public VpnBot(
      @Value("${bot.token}") String botToken,
      @Value("${bot.username}") String username,
      UpdateDispatcher dispatcher,
      TelegramSenderService telegramSenderService) {
    super(botToken);
    this.username = username;
    this.dispatcher = dispatcher;
    telegramSenderService.setAbsSender(this);
  }

  @Override
  public String getBotUsername() {
    return username;
  }

  @Override
  public void onUpdateReceived(Update update) {
    try {
      for (SendMessage message : dispatcher.dispatch(update)) {
        execute(message);
      }
    } catch (TelegramApiException e) {
      System.err.println("Telegram API error: " + e.getMessage());
    } catch (Exception e) {
      System.err.println("Unexpected error in onUpdateReceived: " + e.getMessage());
    }
  }
}
