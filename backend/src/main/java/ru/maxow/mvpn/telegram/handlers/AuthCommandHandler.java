package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;

@Component
@Order(5)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthCommandHandler implements CommandHandler {

  UserService userService;
  TelegramKeyboardFactory keyboardFactory;

  @Override
  public boolean supports(Update update) {
    return update.hasMessage()
        && update.getMessage().hasText()
        && update.getMessage().getText().startsWith("/auth");
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = update.getMessage().getChatId();
    String[] parts = update.getMessage().getText().trim().split("\\s+");

    User existingUser = userService.findByTelegramId(chatId);
    if (existingUser != null) {
      return List.of(
          SendMessage.builder()
              .chatId(chatId.toString())
              .text("✅ Вы уже аутентифицированы.")
              .replyMarkup(keyboardFactory.buildMainMenuKeyboard())
              .build()
      );
    }

    String responseText;
    boolean authenticated = false;
    if (parts.length > 1) {
      String authToken = parts[1];
      try {
        UUID token = UUID.fromString(authToken);
        boolean isValid = userService.checkVerificationCode(token);
        if (isValid) {
          userService.updateUserTelegramId(token, chatId);
          responseText = "✅ Вы успешно аутентифицированы!";
          authenticated = true;
        } else {
          responseText = "❌ Неверный токен аутентификации. Пожалуйста, попробуйте снова.";
        }
      } catch (IllegalArgumentException e) {
        responseText = "❌ Неверный формат токена. Пожалуйста, проверьте правильность ввода.";
      }
    } else {
      responseText = "❌ Пожалуйста, предоставьте токен аутентификации после команды /auth. "
          + "Например: /auth your_token_here";
    }

    return List.of(
        SendMessage.builder()
            .chatId(chatId.toString())
            .text(responseText)
            .replyMarkup(authenticated
                ? keyboardFactory.buildMainMenuKeyboard()
                : keyboardFactory.buildAuthKeyboard())
            .build()
    );
  }
}
