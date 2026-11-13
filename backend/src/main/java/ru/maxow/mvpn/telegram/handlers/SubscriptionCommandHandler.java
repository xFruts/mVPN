package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramLocaleResolver;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.user.User;

@Component
@Order(2)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SubscriptionCommandHandler implements CommandHandler {

  TelegramUserService userService;
  TelegramMessageFormatter messageFormatter;
  TelegramKeyboardFactory keyboardFactory;

  @Override
  public boolean supports(Update update) {
    if (!update.hasMessage() || !update.getMessage().hasText()) {
      return false;
    }
    String text = update.getMessage().getText().trim();
    return text.equals(TelegramKeyboardFactory.BTN_SUBSCRIPTION_RU)
        || text.equals(TelegramKeyboardFactory.BTN_SUBSCRIPTION_EN)
        || text.equals("/status")
        || text.equals("/subscription")
        || text.equals("/sub")
        || text.equals("/my");
  }

  @Override
  public List<BotApiMethod<?>> handle(Update update) {
    Long chatId = update.getMessage().getChatId();
    Locale locale = TelegramLocaleResolver.resolveLocale(update);
    String username = update.getMessage().getFrom() != null
        ? update.getMessage().getFrom().getUserName()
        : null;
    Optional<User> userOpt = userService.findAuthenticatedUser(chatId, username);

    if (userOpt.isEmpty()) {
      return List.of(SendMessage.builder()
          .chatId(chatId.toString())
          .text(messageFormatter.formatUnauthorized(locale))
          .replyMarkup(keyboardFactory.buildAuthKeyboard(locale))
          .build());
    }

    User user = userOpt.get();
    Optional<Subscription> subOpt = userService.findLatestSubscription(user);
    Optional<SubscriptionTrafficState> trafficOpt = subOpt
        .flatMap(s -> userService.getTrafficState(user, s.getId()));

    String responseText = messageFormatter
        .formatSubscriptionStatus(user, subOpt, trafficOpt, locale);

    return List.of(SendMessage.builder()
        .chatId(chatId.toString())
        .text(responseText)
        .replyMarkup(keyboardFactory.buildMainMenuKeyboard(locale))
        .build());
  }
}
