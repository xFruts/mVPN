package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Handler for unknown commands.
 */
@Component
@Order(10)
public class DefaultCommandHandler implements CommandHandler {


  @Override
  public boolean supports(Update update) {
    return update.hasMessage();
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = update.getMessage().getChatId();
    String text = "❓Неизвестная команда. Посмотреть список доступных команд: /help";
    return List.of(
        SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .build()
    );
  }
}
