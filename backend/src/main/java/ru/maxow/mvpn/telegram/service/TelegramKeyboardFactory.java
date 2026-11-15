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

/**
 * Фабрика формирования клавиатур для Telegram-бота (Reply и Inline).
 */
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TelegramKeyboardFactory {

  public static final String KEY_BTN_SUBSCRIPTION = "bot.btn.subscription";
  public static final String KEY_BTN_SERVERS = "bot.btn.servers";
  public static final String KEY_BTN_CONNECTION = "bot.btn.connection";
  public static final String KEY_BTN_HELP = "bot.btn.help";
  public static final String KEY_BTN_DEVICES = "bot.btn.devices";

  private static final List<Locale> SUPPORTED_LOCALES = List.of(
      TelegramLocaleResolver.LOCALE_RU,
      TelegramLocaleResolver.LOCALE_EN
  );

  public static final String CB_GUIDE_HAPP = "guide_happ";
  public static final String CB_GUIDE_V2RAYTUN = "guide_v2raytun";
  public static final String CB_CONNECTION_BACK = "connection_back";
  public static final String CB_GET_VLESS_KEY = "get_vless_key";
  public static final String CB_DEVICES = "user_devices";
  public static final String CB_REFRESH_DEVICES = "refresh_devices";
  public static final String CB_SUBSCRIPTION_BACK = "subscription_back";

  MessageSource messageSource;

  /**
   * Конструктор с внедрением источника локализованных сообщений.
   *
   * @param messageSource источник сообщений
   */
  @Autowired
  public TelegramKeyboardFactory(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Конструктор по умолчанию с базовым ResourceBundleMessageSource.
   */
  public TelegramKeyboardFactory() {
    ResourceBundleMessageSource bundleSource = new ResourceBundleMessageSource();
    bundleSource.setBasenames("messages");
    bundleSource.setDefaultEncoding("UTF-8");
    bundleSource.setUseCodeAsDefaultMessage(true);
    bundleSource.setFallbackToSystemLocale(false);
    this.messageSource = bundleSource;
  }

  /**
   * Проверяет соответствие текста кнопки ключу локализации среди поддерживаемых языков.
   *
   * @param text текст нажатой кнопки
   * @param messageKey ключ локализации
   * @return {@code true}, если текст совпадает с одной из локализаций
   */
  public boolean matchesButton(String text, String messageKey) {
    if (text == null || text.isBlank() || messageKey == null) {
      return false;
    }
    for (Locale locale : SUPPORTED_LOCALES) {
      String expected = getMessage(messageKey, locale);
      if (text.equals(expected)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Создает главное меню кнопок в зависимости от локали пользователя.
   *
   * @param locale локаль пользователя
   * @return разметка клавиатуры главного меню
   */
  public ReplyKeyboardMarkup buildMainMenuKeyboard(Locale locale) {
    Locale target = locale != null ? locale : TelegramLocaleResolver.LOCALE_RU;
    String subText = getMessage(KEY_BTN_SUBSCRIPTION, target);
    String srvText = getMessage(KEY_BTN_SERVERS, target);

    KeyboardRow row1 = new KeyboardRow();
    row1.add(new KeyboardButton(subText));
    row1.add(new KeyboardButton(srvText));

    String connText = getMessage(KEY_BTN_CONNECTION, target);
    String helpText = getMessage(KEY_BTN_HELP, target);

    KeyboardRow row2 = new KeyboardRow();
    row2.add(new KeyboardButton(connText));
    row2.add(new KeyboardButton(helpText));

    return ReplyKeyboardMarkup.builder()
        .keyboard(List.of(row1, row2))
        .resizeKeyboard(true)
        .selective(true)
        .build();
  }

  /**
   * Создает меню кнопок для неавторизованного пользователя.
   *
   * @param locale локаль пользователя
   * @return разметка клавиатуры авторизации
   */
  public ReplyKeyboardMarkup buildAuthKeyboard(Locale locale) {
    Locale target = locale != null ? locale : TelegramLocaleResolver.LOCALE_RU;
    String helpText = getMessage(KEY_BTN_HELP, target);

    KeyboardRow row = new KeyboardRow();
    row.add(new KeyboardButton(helpText));

    return ReplyKeyboardMarkup.builder()
        .keyboard(List.of(row))
        .resizeKeyboard(true)
        .selective(true)
        .build();
  }

  /**
   * Создает inline-клавиатуру для экрана подписки.
   *
   * @param locale локаль пользователя
   * @return разметка inline-клавиатуры подписки
   */
  public InlineKeyboardMarkup buildSubscriptionInlineKeyboard(Locale locale) {
    Locale target = locale != null ? locale : TelegramLocaleResolver.LOCALE_RU;
    String devicesText = getMessage(KEY_BTN_DEVICES, target);

    InlineKeyboardButton devicesBtn = InlineKeyboardButton.builder()
        .text(devicesText)
        .callbackData(CB_DEVICES)
        .build();

    return InlineKeyboardMarkup.builder()
        .keyboard(List.of(List.of(devicesBtn)))
        .build();
  }

  /**
   * Создает inline-клавиатуру списка устройств с кнопками обновления и возврата.
   *
   * @param locale локаль пользователя
   * @return разметка inline-клавиатуры списка устройств
   */
  public InlineKeyboardMarkup buildDevicesInlineKeyboard(Locale locale) {
    Locale target = locale != null ? locale : TelegramLocaleResolver.LOCALE_RU;
    String refreshText = getMessage("bot.btn.devices.refresh", target);
    String backText = getMessage("bot.btn.back.subscription", target);

    InlineKeyboardButton refreshBtn = InlineKeyboardButton.builder()
        .text(refreshText)
        .callbackData(CB_REFRESH_DEVICES)
        .build();

    InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
        .text(backText)
        .callbackData(CB_SUBSCRIPTION_BACK)
        .build();

    return InlineKeyboardMarkup.builder()
        .keyboard(List.of(
            List.of(refreshBtn),
            List.of(backBtn)
        ))
        .build();
  }

  /**
   * Создает inline-клавиатуру для уведомления о новом подключенном устройстве.
   *
   * @param locale локаль пользователя
   * @return разметка inline-клавиатуры уведомления
   */
  public InlineKeyboardMarkup buildNewDeviceNotificationKeyboard(Locale locale) {
    Locale target = locale != null ? locale : TelegramLocaleResolver.LOCALE_RU;
    String devicesText = getMessage(KEY_BTN_DEVICES, target);

    InlineKeyboardButton devicesBtn = InlineKeyboardButton.builder()
        .text(devicesText)
        .callbackData(CB_DEVICES)
        .build();

    return InlineKeyboardMarkup.builder()
        .keyboard(List.of(List.of(devicesBtn)))
        .build();
  }

  /**
   * Создает inline-клавиатуру меню подключения с инструкциями и получением ключа.
   *
   * @param locale локаль пользователя
   * @return разметка inline-клавиатуры подключения
   */
  public InlineKeyboardMarkup buildConnectionInlineKeyboard(Locale locale) {
    Locale target = locale != null ? locale : TelegramLocaleResolver.LOCALE_RU;
    String happText = getMessage("bot.btn.guide.happ", target);
    String v2rayTunText = getMessage("bot.btn.guide.v2raytun", target);
    String keyText = getMessage("bot.btn.key.vless", target);

    InlineKeyboardButton happBtn = InlineKeyboardButton.builder()
        .text(happText)
        .callbackData(CB_GUIDE_HAPP)
        .build();

    InlineKeyboardButton v2rayTunBtn = InlineKeyboardButton.builder()
        .text(v2rayTunText)
        .callbackData(CB_GUIDE_V2RAYTUN)
        .build();

    InlineKeyboardButton keyBtn = InlineKeyboardButton.builder()
        .text(keyText)
        .callbackData(CB_GET_VLESS_KEY)
        .build();

    return InlineKeyboardMarkup.builder()
        .keyboard(List.of(
            List.of(happBtn),
            List.of(v2rayTunBtn),
            List.of(keyBtn)
        ))
        .build();
  }

  /**
   * Создает inline-кнопку возврата в меню подключения из инструкции.
   *
   * @param locale локаль пользователя
   * @return разметка inline-клавиатуры возврата
   */
  public InlineKeyboardMarkup buildGuideBackKeyboard(Locale locale) {
    Locale target = locale != null ? locale : TelegramLocaleResolver.LOCALE_RU;
    String backText = getMessage("bot.btn.back", target);
    InlineKeyboardButton backBtn = InlineKeyboardButton.builder()
        .text(backText)
        .callbackData(CB_CONNECTION_BACK)
        .build();

    return InlineKeyboardMarkup.builder()
        .keyboard(List.of(List.of(backBtn)))
        .build();
  }

  private String getMessage(String code, Locale locale) {
    if (messageSource == null) {
      return code;
    }
    try {
      return messageSource.getMessage(
          code,
          null,
          locale != null ? locale : TelegramLocaleResolver.LOCALE_RU
      );
    } catch (Exception ignored) {
      return code;
    }
  }
}
