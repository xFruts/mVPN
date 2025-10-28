package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;


/**
 * Handler for the /auth command to authenticate users via a token.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class AuthCommandHandler implements CommandHandler {
  private final UserService userService;

  @Override
  public boolean supports(Update update) {
    return update.hasMessage()
        && update.getMessage().hasText()
        && update.getMessage().getText().startsWith("/auth");
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = update.getMessage().getChatId();
    String[] parts = update.getMessage().getText().split(" ");

    User existingUser = userService.findByTelegramId(chatId);
    if (existingUser != null) {
      return List.of(
          SendMessage.builder()
              .chatId(chatId.toString())
              .text("✅ Вы уже аутентифицированы.")
              .build()
      );
    }

    String responseText;
    if (parts.length > 1) {
      String authToken = parts[1];
      try {
        UUID token = UUID.fromString(authToken);
        boolean isAuthenticated = userService.checkVerificationCode(token);
        if (isAuthenticated) {
          userService.updateUserTelegramId(token, chatId);
          responseText = "✅ Вы успешно аутентифицированы!";
        } else {
          responseText = "❌ Неверный токен аутентификации. Пожалуйста, попробуйте снова.";
        }
      } catch (IllegalArgumentException e) {
        responseText = "❌ Неверный формат токена. Пожалуйста, проверьте правильность ввода.";
      }
    } else {
      responseText =
          "❌ Пожалуйста, предоставьте токен аутентификации после команды /auth. "
              + "Например: /auth your_token_here";
    }
    return List.of(
        SendMessage.builder()
            .chatId(chatId.toString())
            .text(responseText)
            .build()
    );
  }
}
