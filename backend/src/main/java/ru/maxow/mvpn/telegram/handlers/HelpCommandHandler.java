package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Handler for /help command.
 */
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
            Данный бот предназначен для предоставления доступа к VPN-сервису.
            Для получения доступа необходимо авторизоваться с помощью кода,
            который можно получить у администратора своего vpn сервиса.
            После успешной авторизации вам будут доступны команды для управления вашим VPN-доступом.
            Если у вас возникнут вопросы, пожалуйста, обратитесь к администратору.
            
            Список доступных команд:
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
