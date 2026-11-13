package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.user.User;

@Component
@Order(3)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServersCommandHandler implements CommandHandler {

  TelegramUserService userService;
  TelegramMessageFormatter messageFormatter;
  TelegramKeyboardFactory keyboardFactory;

  @Override
  public boolean supports(Update update) {
    if (!update.hasMessage() || !update.getMessage().hasText()) {
      return false;
    }
    String text = update.getMessage().getText().trim();
    return text.equals(TelegramKeyboardFactory.BTN_SERVERS)
        || text.equals("/servers")
        || text.equals("/countries")
        || text.equals("/nodes");
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = update.getMessage().getChatId();
    String username = update.getMessage().getFrom() != null
        ? update.getMessage().getFrom().getUserName()
        : null;
    Optional<User> userOpt = userService.findAuthenticatedUser(chatId, username);

    if (userOpt.isEmpty()) {
      return List.of(SendMessage.builder()
          .chatId(chatId.toString())
          .text(messageFormatter.formatUnauthorized())
          .replyMarkup(keyboardFactory.buildAuthKeyboard())
          .build());
    }

    User user = userOpt.get();
    List<Server> servers = userService.getAvailableServers(user);
    String responseText = messageFormatter.formatServersStatus(servers);

    return List.of(SendMessage.builder()
        .chatId(chatId.toString())
        .text(responseText)
        .replyMarkup(keyboardFactory.buildMainMenuKeyboard())
        .build());
  }
}
