package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
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
import ru.maxow.mvpn.user.User;

@Component
@Order(1)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StartCommandHandler implements CommandHandler {

  TelegramUserService userService;
  TelegramMessageFormatter messageFormatter;
  TelegramKeyboardFactory keyboardFactory;

  @Override
  public boolean supports(Update update) {
    return update.hasMessage()
        && update.getMessage().hasText()
        && update.getMessage().getText().startsWith("/start");
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = update.getMessage().getChatId();
    Locale locale = TelegramLocaleResolver.resolveLocale(update);
    String firstName = update.getMessage().getFrom() != null
        ? update.getMessage().getFrom().getFirstName()
        : null;
    String username = update.getMessage().getFrom() != null
        ? update.getMessage().getFrom().getUserName()
        : null;

    String[] parts = update.getMessage().getText().trim().split("\\s+");
    if (parts.length > 1) {
      String tokenStr = parts[1];
      try {
        UUID token = UUID.fromString(tokenStr);
        boolean authenticated = userService.authenticateUser(token, chatId);
        if (authenticated) {
          String text = messageFormatter.formatAuthSuccess(locale)
              + "\n\n"
              + messageFormatter.formatWelcome(firstName, true, locale);
          return List.of(SendMessage.builder()
              .chatId(chatId.toString())
              .text(text)
              .replyMarkup(keyboardFactory.buildMainMenuKeyboard(locale))
              .build());
        }
      } catch (IllegalArgumentException ignored) {
        // Невалидный UUID переходит к проверке по имени
      }
    }

    Optional<User> userOpt = userService.findAuthenticatedUser(chatId, username);
    boolean isAuthenticated = userOpt.isPresent();
    String text = messageFormatter.formatWelcome(firstName, isAuthenticated, locale);

    return List.of(SendMessage.builder()
        .chatId(chatId.toString())
        .text(text)
        .replyMarkup(isAuthenticated
            ? keyboardFactory.buildMainMenuKeyboard(locale)
            : keyboardFactory.buildAuthKeyboard(locale))
        .build());
  }
}
