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
import ru.maxow.mvpn.telegram.model.UserDevicesSummary;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramLocaleResolver;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.telegram.util.TelegramConstants;
import ru.maxow.mvpn.telegram.util.TelegramUpdateUtils;
import ru.maxow.mvpn.user.User;

/**
 * Обработчик команды просмотра и обновления списка активных устройств (/devices).
 */
@Component
@Order(3)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DevicesCommandHandler implements CommandHandler {

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
      return TelegramKeyboardFactory.CB_DEVICES.equals(data)
          || TelegramKeyboardFactory.CB_REFRESH_DEVICES.equals(data);
    }
    if (!update.hasMessage() || !update.getMessage().hasText()) {
      return false;
    }
    String text = update.getMessage().getText().trim();
    return keyboardFactory.matchesButton(text, TelegramKeyboardFactory.KEY_BTN_DEVICES)
        || text.equals("/devices")
        || text.equals("/device")
        || text.equals("/ips")
        || text.equals("/ip");
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
    if (subOpt.isEmpty() || !subOpt.get().isActive()) {
      return List.of(buildResponse(
          chatId,
          messageId,
          isCallback,
          messageFormatter.formatNoActiveSubscription(locale),
          null
      ));
    }

    InlineKeyboardMarkup keyboard = keyboardFactory.buildDevicesInlineKeyboard(locale);
    UserDevicesSummary summary = userService.getConnectedDevices(user);
    String responseText = messageFormatter.formatDevices(summary, locale);
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
      EditMessageText.EditMessageTextBuilder builder = EditMessageText.builder()
          .chatId(chatId.toString())
          .messageId(messageId)
          .text(text)
          .parseMode(TelegramConstants.PARSE_MODE_MARKDOWN);
      if (keyboard != null) {
        builder.replyMarkup(keyboard);
      }
      return builder.build();
    }
    SendMessage.SendMessageBuilder builder = SendMessage.builder()
        .chatId(chatId.toString())
        .text(text)
        .parseMode(TelegramConstants.PARSE_MODE_MARKDOWN);
    if (keyboard != null) {
      builder.replyMarkup(keyboard);
    }
    return builder.build();
  }
}
