package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import java.util.Locale;
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
import ru.maxow.mvpn.telegram.service.TelegramLocaleResolver;
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
    if (update.hasCallbackQuery()) {
      String data = update.getCallbackQuery().getData();
      return TelegramKeyboardFactory.CB_GUIDE_HAPP.equals(data)
          || TelegramKeyboardFactory.CB_GUIDE_V2RAYTUN.equals(data)
          || TelegramKeyboardFactory.CB_GET_VLESS_KEY.equals(data);
    }
    if (!update.hasMessage() || !update.getMessage().hasText()) {
      return false;
    }
    String text = update.getMessage().getText().trim();
    return text.equals(TelegramKeyboardFactory.BTN_CONNECTION_RU)
        || text.equals(TelegramKeyboardFactory.BTN_CONNECTION_EN)
        || text.equals("/connect")
        || text.equals("/config")
        || text.equals("/link")
        || text.equals("/getkey")
        || text.equals("/key")
        || text.equals("/vless")
        || text.equals("/happ")
        || text.equals("/v2raytun");
  }

  @Override
  public List<SendMessage> handle(Update update) {
    Long chatId = extractChatId(update);
    Locale locale = TelegramLocaleResolver.resolveLocale(update);
    String action = extractAction(update);
    String username = extractUsername(update);

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
    if (subOpt.isEmpty() || !subOpt.get().isActive()) {
      return List.of(SendMessage.builder()
          .chatId(chatId.toString())
          .text(messageFormatter.formatNoActiveSubscription(locale))
          .replyMarkup(keyboardFactory.buildMainMenuKeyboard(locale))
          .build());
    }

    String subscriptionUrl = userService.buildSubscriptionUrl(user);

    if (isHappRequest(action)) {
      return List.of(SendMessage.builder()
          .chatId(chatId.toString())
          .text(messageFormatter.formatHappGuide(subscriptionUrl, locale))
          .replyMarkup(keyboardFactory.buildConnectionInlineKeyboard(locale))
          .build());
    }

    if (isV2rayTunRequest(action)) {
      return List.of(SendMessage.builder()
          .chatId(chatId.toString())
          .text(messageFormatter.formatV2rayTunGuide(subscriptionUrl, locale))
          .replyMarkup(keyboardFactory.buildConnectionInlineKeyboard(locale))
          .build());
    }

    if (isVlessKeyRequest(action)) {
      Optional<String> vlessKeyOpt = userService.getDirectVlessKey(user);
      return List.of(SendMessage.builder()
          .chatId(chatId.toString())
          .text(messageFormatter.formatVlessKey(vlessKeyOpt, subscriptionUrl, locale))
          .replyMarkup(keyboardFactory.buildConnectionInlineKeyboard(locale))
          .build());
    }

    String responseText = messageFormatter.formatConnectionGuide(
        user, subOpt, subscriptionUrl, locale
    );
    return List.of(SendMessage.builder()
        .chatId(chatId.toString())
        .text(responseText)
        .replyMarkup(keyboardFactory.buildConnectionInlineKeyboard(locale))
        .build());
  }

  private Long extractChatId(Update update) {
    if (update.hasCallbackQuery()) {
      return update.getCallbackQuery().getMessage().getChatId();
    }
    return update.getMessage().getChatId();
  }

  private String extractAction(Update update) {
    if (update.hasCallbackQuery()) {
      return update.getCallbackQuery().getData();
    }
    return update.getMessage().getText().trim();
  }

  private String extractUsername(Update update) {
    if (update.hasCallbackQuery() && update.getCallbackQuery().getFrom() != null) {
      return update.getCallbackQuery().getFrom().getUserName();
    }
    if (update.hasMessage() && update.getMessage().getFrom() != null) {
      return update.getMessage().getFrom().getUserName();
    }
    return null;
  }

  private boolean isHappRequest(String action) {
    return "/happ".equals(action)
        || TelegramKeyboardFactory.CB_GUIDE_HAPP.equals(action);
  }

  private boolean isV2rayTunRequest(String action) {
    return "/v2raytun".equals(action)
        || TelegramKeyboardFactory.CB_GUIDE_V2RAYTUN.equals(action);
  }

  private boolean isVlessKeyRequest(String action) {
    return "/key".equals(action)
        || "/vless".equals(action)
        || "/getkey".equals(action)
        || TelegramKeyboardFactory.CB_GET_VLESS_KEY.equals(action);
  }
}
