package ru.maxow.mvpn.telegram.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "bot", name = "enabled", havingValue = "true")
public class VpnBot extends TelegramLongPollingBot {

  private final UpdateDispatcher dispatcher;
  private final String username;

  public VpnBot(
      @Value("${bot.token}") String botToken,
      @Value("${bot.username}") String username,
      UpdateDispatcher dispatcher,
      TelegramSenderService telegramSenderService
  ) {
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
      if (update.hasCallbackQuery()) {
        try {
          execute(AnswerCallbackQuery.builder()
              .callbackQueryId(update.getCallbackQuery().getId())
              .build());
        } catch (Exception e) {
          log.debug("Failed to answer callback query: {}", e.getMessage());
        }
      }
      for (BotApiMethod<?> message : dispatcher.dispatch(update)) {
        try {
          execute(message);
        } catch (TelegramApiException e) {
          if (e.getMessage() != null && e.getMessage().contains("message is not modified")) {
            log.debug("Message content unchanged, skipping edit: {}", e.getMessage());
          } else {
            throw e;
          }
        }
      }
    } catch (TelegramApiException e) {
      log.error("Telegram API error: {}", e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error in onUpdateReceived: {}", e.getMessage(), e);
    }
  }
}
