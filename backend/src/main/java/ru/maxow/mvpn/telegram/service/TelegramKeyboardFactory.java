package ru.maxow.mvpn.telegram.service;

import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TelegramKeyboardFactory {

  public static final String BTN_SUBSCRIPTION_RU = "📊 Моя подписка";
  public static final String BTN_SUBSCRIPTION_EN = "📊 My Subscription";
  public static final String BTN_SERVERS_RU = "🌐 Доступные сервера";
  public static final String BTN_SERVERS_EN = "🌐 Available Servers";
  public static final String BTN_CONNECTION_RU = "🔗 Подключение";
  public static final String BTN_CONNECTION_EN = "🔗 Connection";
  public static final String BTN_HELP_RU = "❓ Помощь";
  public static final String BTN_HELP_EN = "❓ Help";

  // Backward-compatible aliases
  public static final String BTN_SUBSCRIPTION = BTN_SUBSCRIPTION_RU;
  public static final String BTN_SERVERS = BTN_SERVERS_RU;
  public static final String BTN_CONNECTION = BTN_CONNECTION_RU;
  public static final String BTN_HELP = BTN_HELP_RU;

  public static final String CB_GUIDE_HAPP = "guide_happ";
  public static final String CB_GUIDE_V2RAYTUN = "guide_v2raytun";
  public static final String CB_GET_VLESS_KEY = "get_vless_key";

  MessageSource messageSource;

  @Autowired
  public TelegramKeyboardFactory(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public TelegramKeyboardFactory() {
    ResourceBundleMessageSource bundleSource = new ResourceBundleMessageSource();
    bundleSource.setBasenames("messages");
    bundleSource.setDefaultEncoding("UTF-8");
    bundleSource.setUseCodeAsDefaultMessage(true);
    bundleSource.setFallbackToSystemLocale(false);
    this.messageSource = bundleSource;
  }

  public ReplyKeyboardMarkup buildMainMenuKeyboard() {
    return buildMainMenuKeyboard(TelegramLocaleResolver.LOCALE_RU);
  }

  public ReplyKeyboardMarkup buildMainMenuKeyboard(Locale locale) {
    Locale target = locale != null ? locale : TelegramLocaleResolver.LOCALE_RU;
    String subText = getMessage("bot.btn.subscription", target, BTN_SUBSCRIPTION_RU);
    String srvText = getMessage("bot.btn.servers", target, BTN_SERVERS_RU);
    String connText = getMessage("bot.btn.connection", target, BTN_CONNECTION_RU);
    String helpText = getMessage("bot.btn.help", target, BTN_HELP_RU);

    KeyboardRow row1 = new KeyboardRow();
    row1.add(new KeyboardButton(subText));
    row1.add(new KeyboardButton(srvText));

    KeyboardRow row2 = new KeyboardRow();
    row2.add(new KeyboardButton(connText));
    row2.add(new KeyboardButton(helpText));

    return ReplyKeyboardMarkup.builder()
        .keyboard(List.of(row1, row2))
        .resizeKeyboard(true)
        .selective(true)
        .build();
  }

  public ReplyKeyboardMarkup buildAuthKeyboard() {
    return buildAuthKeyboard(TelegramLocaleResolver.LOCALE_RU);
  }

  public ReplyKeyboardMarkup buildAuthKeyboard(Locale locale) {
    Locale target = locale != null ? locale : TelegramLocaleResolver.LOCALE_RU;
    String helpText = getMessage("bot.btn.help", target, BTN_HELP_RU);

    KeyboardRow row = new KeyboardRow();
    row.add(new KeyboardButton(helpText));

    return ReplyKeyboardMarkup.builder()
        .keyboard(List.of(row))
        .resizeKeyboard(true)
        .selective(true)
        .build();
  }

  public InlineKeyboardMarkup buildConnectionInlineKeyboard() {
    return buildConnectionInlineKeyboard(TelegramLocaleResolver.LOCALE_RU);
  }

  public InlineKeyboardMarkup buildConnectionInlineKeyboard(Locale locale) {
    Locale target = locale != null ? locale : TelegramLocaleResolver.LOCALE_RU;
    String happText = getMessage(
        "bot.btn.guide.happ", target, "📱 Инструкция: Happ"
    );
    String v2rayTunText = getMessage(
        "bot.btn.guide.v2raytun", target, "📱 Инструкция: v2rayTun"
    );
    String vlessKeyText = getMessage(
        "bot.btn.key.vless", target, "🔑 Скопировать VLESS-ключ"
    );

    InlineKeyboardButton happBtn = InlineKeyboardButton.builder()
        .text(happText)
        .callbackData(CB_GUIDE_HAPP)
        .build();

    InlineKeyboardButton v2rayTunBtn = InlineKeyboardButton.builder()
        .text(v2rayTunText)
        .callbackData(CB_GUIDE_V2RAYTUN)
        .build();

    InlineKeyboardButton vlessKeyBtn = InlineKeyboardButton.builder()
        .text(vlessKeyText)
        .callbackData(CB_GET_VLESS_KEY)
        .build();

    return InlineKeyboardMarkup.builder()
        .keyboard(List.of(
            List.of(happBtn),
            List.of(v2rayTunBtn),
            List.of(vlessKeyBtn)
        ))
        .build();
  }

  private String getMessage(String code, Locale locale, String defaultMessage) {
    if (messageSource == null) {
      return defaultMessage;
    }
    return messageSource.getMessage(code, null, defaultMessage, locale);
  }
}
