package ru.maxow.mvpn.features;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.adapter.telegram.CommandHandler;

import java.util.List;

@Component
@Order(1)
public class HelpCommandHandler implements CommandHandler {
  @Override
  public boolean supports(Update update) {
    return update.hasMessage()
        && update.getMessage().hasText()
        && update.getMessage().getText().startsWith("/help");
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = update.getMessage().getChatId();

    String text = """
            Список доступных команд:
            /start - начать работу с ботом
            /auth - Ввести код авторизации, полученный от администратора
            """;
    return List.of(
        SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .build()
    );
  }
}
