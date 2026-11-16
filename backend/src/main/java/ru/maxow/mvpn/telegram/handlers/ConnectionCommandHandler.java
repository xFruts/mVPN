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
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramLocaleResolver;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.telegram.util.TelegramConstants;
import ru.maxow.mvpn.telegram.util.TelegramUpdateUtils;
import ru.maxow.mvpn.user.User;

/**
 * Обработчик команд меню подключения, инструкций клиентов и VLESS-ключей.
 */
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
    if (update == null) {
      return false;
    }
    if (update.hasCallbackQuery()) {
      String data = update.getCallbackQuery().getData();
      return TelegramKeyboardFactory.CB_GUIDE_HAPP.equals(data)
          || TelegramKeyboardFactory.CB_GUIDE_V2RAYTUN.equals(data)
          || TelegramKeyboardFactory.CB_CONNECTION_BACK.equals(data)
          || TelegramKeyboardFactory.CB_GET_VLESS_KEY.equals(data);
    }
    if (!update.hasMessage() || !update.getMessage().hasText()) {
      return false;
    }
    String text = update.getMessage().getText().trim();
    return TelegramConstants.CONNECTION_COMMANDS.contains(text)
        || keyboardFactory.matchesButton(text, TelegramKeyboardFactory.KEY_BTN_CONNECTION);
  }

  @Override
  public List<BotApiMethod<?>> handle(Update update) {
    // Шаг 1: Извлечение метаданных обновления
    Long chatId = TelegramUpdateUtils.extractChatId(update);
    Locale locale = TelegramLocaleResolver.resolveLocale(update);
    String action = extractAction(update);
    String username = TelegramUpdateUtils.extractUsername(update);
    boolean isCallback = update.hasCallbackQuery();
    Integer messageId = isCallback
        ? update.getCallbackQuery().getMessage().getMessageId()
        : null;

    // Шаг 2: Проверка аутентификации пользователя
    Optional<User> userOpt = userService.findAuthenticatedUser(chatId, username);
    if (userOpt.isEmpty()) {
      return List.of(SendMessage.builder()
          .chatId(chatId.toString())
          .text(messageFormatter.formatUnauthorized(locale))
          .replyMarkup(keyboardFactory.buildAuthKeyboard(locale))
          .build());
    }

    // Шаг 3: Проверка наличия и активности подписки
    User user = userOpt.get();
    Optional<Subscription> subOpt = userService.findLatestSubscription(user);
    if (subOpt.isEmpty() || !subOpt.get().isActive()) {
      return List.of(SendMessage.builder()
          .chatId(chatId.toString())
          .text(messageFormatter.formatNoActiveSubscription(locale))
          .replyMarkup(keyboardFactory.buildMainMenuKeyboard(locale))
          .build());
    }

    // Шаг 4: Формирование ссылки на подписку
    String subscriptionUrl = userService.buildSubscriptionUrl(user);

    // Шаг 5: Маршрутизация по действиям (инструкции Happ / v2rayTun)
    if (isHappRequest(action)) {
      String text = messageFormatter.formatHappGuide(subscriptionUrl, locale);
      InlineKeyboardMarkup kb = keyboardFactory.buildGuideBackKeyboard(locale);
      return List.of(buildResponse(chatId, messageId, isCallback, text, kb));
    }

    if (isV2rayTunRequest(action)) {
      String text = messageFormatter.formatV2rayTunGuide(subscriptionUrl, locale);
      InlineKeyboardMarkup kb = keyboardFactory.buildGuideBackKeyboard(locale);
      return List.of(buildResponse(chatId, messageId, isCallback, text, kb));
    }

    // Шаг 6: Получение прямого VLESS-ключа
    if (isVlessKeyRequest(action)) {
      Optional<String> vlessKeyOpt = userService.getDirectVlessKey(user);
      String text = messageFormatter.formatVlessKey(
          vlessKeyOpt.orElse(null), subscriptionUrl, locale
      );
      InlineKeyboardMarkup kb = keyboardFactory.buildGuideBackKeyboard(locale);
      return List.of(buildResponse(chatId, messageId, isCallback, text, kb));
    }

    // Шаг 7: Экран по умолчанию (главное меню подключения)
    String responseText = messageFormatter.formatConnectionGuide(
        subscriptionUrl, locale
    );
    InlineKeyboardMarkup keyboard = keyboardFactory.buildConnectionInlineKeyboard(locale);
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

  private String extractAction(Update update) {
    if (update.hasCallbackQuery()) {
      return update.getCallbackQuery().getData();
    }
    return update.getMessage().getText().trim();
  }

  private boolean isHappRequest(String action) {
    return TelegramConstants.CMD_HAPP.equals(action)
        || TelegramKeyboardFactory.CB_GUIDE_HAPP.equals(action);
  }

  private boolean isV2rayTunRequest(String action) {
    return TelegramConstants.CMD_V2RAYTUN.equals(action)
        || TelegramKeyboardFactory.CB_GUIDE_V2RAYTUN.equals(action);
  }

  private boolean isVlessKeyRequest(String action) {
    return TelegramConstants.CMD_KEY.equals(action)
        || TelegramConstants.CMD_VLESS.equals(action)
        || TelegramConstants.CMD_GETKEY.equals(action)
        || TelegramKeyboardFactory.CB_GET_VLESS_KEY.equals(action);
  }
}
