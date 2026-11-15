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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramLocaleResolver;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.telegram.util.TelegramConstants;
import ru.maxow.mvpn.telegram.util.TelegramUpdateUtils;
import ru.maxow.mvpn.user.User;

/**
 * Обработчик команд просмотра статуса подписки и остатка трафика (/status).
 */
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
    if (update == null) {
      return false;
    }
    if (update.hasCallbackQuery()) {
      String data = update.getCallbackQuery().getData();
      return TelegramKeyboardFactory.CB_SUBSCRIPTION_BACK.equals(data);
    }
    if (!update.hasMessage() || !update.getMessage().hasText()) {
      return false;
    }
    String text = update.getMessage().getText().trim();
    return keyboardFactory.matchesButton(text, TelegramKeyboardFactory.KEY_BTN_SUBSCRIPTION)
        || text.equals("/status")
        || text.equals("/subscription")
        || text.equals("/sub")
        || text.equals("/my");
  }

  @Override
  public List<BotApiMethod<?>> handle(Update update) {
    Long chatId = TelegramUpdateUtils.extractChatId(update);
    Locale locale = TelegramLocaleResolver.resolveLocale(update);
    String username = TelegramUpdateUtils.extractUsername(update);
    boolean isCallback = update.hasCallbackQuery();
    Integer messageId = isCallback
        ? update.getCallbackQuery().getMessage().getMessageId()
        : null;

    Optional<User> userOpt = userService.findAuthenticatedUser(chatId, username);
    if (userOpt.isEmpty()) {
      return List.of(buildResponse(
          chatId,
          messageId,
          isCallback,
          messageFormatter.formatUnauthorized(locale),
          null
      ));
    }

    User user = userOpt.get();
    Optional<Subscription> subOpt = userService.findLatestSubscription(user);
    if (subOpt.isEmpty()) {
      return List.of(buildResponse(
          chatId,
          messageId,
          isCallback,
          messageFormatter.formatNoActiveSubscription(locale),
          null
      ));
    }

    Subscription sub = subOpt.get();
    Optional<SubscriptionTrafficState> trafficOpt =
        userService.getTrafficState(user, sub.getId());

    String responseText = messageFormatter
        .formatSubscriptionStatus(sub, trafficOpt.orElse(null), locale);
    InlineKeyboardMarkup keyboard = keyboardFactory.buildSubscriptionInlineKeyboard(locale);

    return List.of(buildResponse(chatId, messageId, isCallback, responseText, keyboard));
  }

  private BotApiMethod<?> buildResponse(
      Long chatId,
      Integer messageId,
      boolean isCallback,
      String text,
      InlineKeyboardMarkup keyboard
  ) {
    if (isCallback && messageId != null) {
      return EditMessageText.builder()
          .chatId(chatId.toString())
          .messageId(messageId)
          .text(text)
          .parseMode(TelegramConstants.PARSE_MODE_MARKDOWN)
          .replyMarkup(keyboard)
          .build();
    }
    return SendMessage.builder()
        .chatId(chatId.toString())
        .text(text)
        .parseMode(TelegramConstants.PARSE_MODE_MARKDOWN)
        .replyMarkup(keyboard)
        .build();
  }
}
