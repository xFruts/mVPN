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
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.user.User;

@Component
@Order(4)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConnectionCommandHandler implements CommandHandler {

  TelegramUserService userService;
  TelegramMessageFormatter messageFormatter;
  TelegramKeyboardFactory keyboardFactory;

  @Override
  public boolean supports(Update update) {
    if (!update.hasMessage() || !update.getMessage().hasText()) {
      return false;
    }
    String text = update.getMessage().getText().trim();
    return text.equals(TelegramKeyboardFactory.BTN_CONNECTION)
        || text.equals("/connect")
        || text.equals("/config")
        || text.equals("/link")
        || text.equals("/getkey");
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = update.getMessage().getChatId();
    Optional<User> userOpt = userService.findAuthenticatedUser(chatId);

    if (userOpt.isEmpty()) {
      String text = """
          ⚠️ Вы ещё не авторизованы.

          Для привязки аккаунта введите:
          `/auth <ваш_токен>`
          (токен можно получить в личном кабинете mVPN)
          """;
      return List.of(SendMessage.builder()
          .chatId(chatId.toString())
          .text(text)
          .replyMarkup(keyboardFactory.buildAuthKeyboard())
          .build());
    }

    User user = userOpt.get();
    Optional<Subscription> subOpt = userService.findLatestSubscription(user);
    String subscriptionUrl = userService.buildSubscriptionUrl(user);
    String responseText = messageFormatter.formatConnectionGuide(
        user, subOpt, subscriptionUrl);

    return List.of(SendMessage.builder()
        .chatId(chatId.toString())
        .text(responseText)
        .replyMarkup(keyboardFactory.buildMainMenuKeyboard())
        .build());
  }
}
