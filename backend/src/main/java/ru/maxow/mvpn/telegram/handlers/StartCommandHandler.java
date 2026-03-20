package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Handler for /start command.
 */
@Component
@Order(1)
public class StartCommandHandler implements CommandHandler {

  @Override
  public boolean supports(Update update) {
    return update.hasMessage()
        && update.getMessage().hasText()
        && update.getMessage().getText().startsWith("/start");
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = update.getMessage().getChatId();
    String firstName = update.getMessage().getFrom() != null
        ? update.getMessage().getFrom().getFirstName()
        : null;

    String text = String.format(
        "👋Привет %s! Я бот, который поможет получить доступ к VPN.%n"
            + "ℹ️Подробнее обо мне и как подключить VPN в /help%n",
        firstName != null ? ", " + firstName : " друг"
    );
    return List.of(
        SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .build()
    );
  }
}
