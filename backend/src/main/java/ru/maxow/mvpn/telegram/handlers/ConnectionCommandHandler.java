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
import ru.maxow.mvpn.server.Server;
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
          || TelegramKeyboardFactory.CB_SELECT_COUNTRY.equals(data)
          || TelegramKeyboardFactory.CB_CONNECTION_BACK.equals(data)
          || TelegramKeyboardFactory.CB_GET_VLESS_KEY.equals(data)
          || (data != null && data.startsWith(TelegramKeyboardFactory.CB_COUNTRY_PREFIX));
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
        || text.equals("/v2raytun")
        || text.equals("/country")
        || text.equals("/countries");
  }

  @Override
  public List<BotApiMethod<?>> handle(Update update) {
    Long chatId = extractChatId(update);
    Locale locale = TelegramLocaleResolver.resolveLocale(update);
    String action = extractAction(update);
    String username = extractUsername(update);
    boolean isCallback = update.hasCallbackQuery();
    Integer messageId = isCallback
        ? update.getCallbackQuery().getMessage().getMessageId()
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
    if (subOpt.isEmpty() || !subOpt.get().isActive()) {
      return List.of(SendMessage.builder()
          .chatId(chatId.toString())
          .text(messageFormatter.formatNoActiveSubscription(locale))
          .replyMarkup(keyboardFactory.buildMainMenuKeyboard(locale))
          .build());
    }

    String subscriptionUrl = userService.buildSubscriptionUrl(user);

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

    if (isSelectCountryRequest(action)) {
      List<Server> servers = userService.getAvailableServers(user);
      String text = messageFormatter.formatCountrySelection(locale);
      InlineKeyboardMarkup kb = keyboardFactory.buildCountrySelectionKeyboard(servers, locale);
      return List.of(buildResponse(chatId, messageId, isCallback, text, kb));
    }

    if (isCountryConfigRequest(action)) {
      Long serverId = parseServerId(action);
      Optional<Server> serverOpt = userService.findServerById(serverId);
      if (serverOpt.isPresent()) {
        Server server = serverOpt.get();
        Optional<String> configOpt = userService.getServerConfig(server, user);
        String text = messageFormatter.formatCountryConfig(server, configOpt, locale);
        InlineKeyboardMarkup kb = keyboardFactory.buildCountryConfigKeyboard(locale);
        return List.of(buildResponse(chatId, messageId, isCallback, text, kb));
      }
    }

    if (isVlessKeyRequest(action)) {
      Optional<String> vlessKeyOpt = userService.getDirectVlessKey(user);
      String text = messageFormatter.formatVlessKey(vlessKeyOpt, subscriptionUrl, locale);
      InlineKeyboardMarkup kb = keyboardFactory.buildGuideBackKeyboard(locale);
      return List.of(buildResponse(chatId, messageId, isCallback, text, kb));
    }

    String responseText = messageFormatter.formatConnectionGuide(
        user, subOpt, subscriptionUrl, locale
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
          .replyMarkup(keyboard)
          .build();
    }
    return SendMessage.builder()
        .chatId(chatId.toString())
        .text(text)
        .replyMarkup(keyboard)
        .build();
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

  private boolean isSelectCountryRequest(String action) {
    return "/country".equals(action)
        || "/countries".equals(action)
        || TelegramKeyboardFactory.CB_SELECT_COUNTRY.equals(action);
  }

  private boolean isCountryConfigRequest(String action) {
    return action != null && action.startsWith(TelegramKeyboardFactory.CB_COUNTRY_PREFIX);
  }

  private Long parseServerId(String action) {
    try {
      return Long.parseLong(action.substring(TelegramKeyboardFactory.CB_COUNTRY_PREFIX.length()));
    } catch (Exception e) {
      return null;
    }
  }

  private boolean isVlessKeyRequest(String action) {
    return "/key".equals(action)
        || "/vless".equals(action)
        || "/getkey".equals(action)
        || TelegramKeyboardFactory.CB_GET_VLESS_KEY.equals(action);
  }
}
