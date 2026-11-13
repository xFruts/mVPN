package ru.maxow.mvpn.telegram.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
      for (SendMessage message : dispatcher.dispatch(update)) {
        execute(message);
      }
    } catch (TelegramApiException e) {
      log.error("Telegram API error: {}", e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error in onUpdateReceived: {}", e.getMessage(), e);
    }
  }
}
