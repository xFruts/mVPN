package ru.maxow.mvpn.features;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.adapter.telegram.CommandHandler;

import java.util.List;

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

    String text = String.format("""
            👋Привет %s! Я бот, который поможет получить доступ к VPN.
            ℹ️Подробнее обо мне и как подключить VPN в /help
            """, firstName != null ? ", " + firstName : " друг");
    return List.of(
        SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .build()
    );
  }
}
