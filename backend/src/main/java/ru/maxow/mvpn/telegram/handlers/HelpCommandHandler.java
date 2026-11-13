package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;

@Component
@Order(6)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HelpCommandHandler implements CommandHandler {

  TelegramUserService userService;
  TelegramMessageFormatter messageFormatter;
  TelegramKeyboardFactory keyboardFactory;

  @Override
  public boolean supports(Update update) {
    if (!update.hasMessage() || !update.getMessage().hasText()) {
      return false;
    }
    String text = update.getMessage().getText().trim();
    return text.equals(TelegramKeyboardFactory.BTN_HELP) || text.startsWith("/help");
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = update.getMessage().getChatId();
    boolean isAuthenticated = userService.findAuthenticatedUser(chatId).isPresent();
    String text = messageFormatter.formatHelp(isAuthenticated);

    return List.of(
        SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .replyMarkup(isAuthenticated
                ? keyboardFactory.buildMainMenuKeyboard()
                : keyboardFactory.buildAuthKeyboard())
            .build()
    );
  }
}
