package ru.maxow.mvpn.adapter.telegram;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class VPNBot extends TelegramLongPollingBot {

    private final UpdateDispatcher dispatcher;
    private final String username;

    public VPNBot(
            @Value("${bot.token}") String botToken,
            @Value("${bot.username}") String username,
            UpdateDispatcher dispatcher) {
        super(botToken);
        this.username = username;
        this.dispatcher = dispatcher;
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
