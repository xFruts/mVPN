package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramLocaleResolver;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;

@Component
@Order(100)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DefaultCommandHandler implements CommandHandler {

  TelegramUserService userService;
  TelegramMessageFormatter messageFormatter;
  TelegramKeyboardFactory keyboardFactory;

  @Override
  public boolean supports(Update update) {
    return update.hasMessage();
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = update.getMessage().getChatId();
    Locale locale = TelegramLocaleResolver.resolveLocale(update);
    String username = update.getMessage().getFrom() != null
        ? update.getMessage().getFrom().getUserName()
        : null;
    boolean isAuthenticated = userService.findAuthenticatedUser(chatId, username).isPresent();
    String text = messageFormatter.formatUnknownCommand(locale);

    return List.of(
        SendMessage.builder()
            .chatId(chatId.toString())
            .text(text)
            .replyMarkup(isAuthenticated
                ? keyboardFactory.buildMainMenuKeyboard(locale)
                : keyboardFactory.buildAuthKeyboard(locale))
            .build()
    );
  }
}
