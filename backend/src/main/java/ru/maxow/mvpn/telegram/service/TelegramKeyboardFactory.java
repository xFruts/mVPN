package ru.maxow.mvpn.telegram.service;

import java.util.List;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
public class TelegramKeyboardFactory {

  public static final String BTN_SUBSCRIPTION = "📊 Моя подписка";
  public static final String BTN_SERVERS = "🌐 Доступные сервера";
  public static final String BTN_CONNECTION = "🔗 Подключение";
  public static final String BTN_HELP = "❓ Помощь";

  public static final String CB_GUIDE_HAPP = "guide_happ";
  public static final String CB_GUIDE_V2RAYTUN = "guide_v2raytun";
  public static final String CB_GET_VLESS_KEY = "get_vless_key";

  public ReplyKeyboardMarkup buildMainMenuKeyboard() {
    KeyboardRow row1 = new KeyboardRow();
    row1.add(new KeyboardButton(BTN_SUBSCRIPTION));
    row1.add(new KeyboardButton(BTN_SERVERS));

    KeyboardRow row2 = new KeyboardRow();
    row2.add(new KeyboardButton(BTN_CONNECTION));
    row2.add(new KeyboardButton(BTN_HELP));

    return ReplyKeyboardMarkup.builder()
        .keyboard(List.of(row1, row2))
        .resizeKeyboard(true)
        .selective(true)
        .build();
  }

  public ReplyKeyboardMarkup buildAuthKeyboard() {
    KeyboardRow row = new KeyboardRow();
    row.add(new KeyboardButton(BTN_HELP));

    return ReplyKeyboardMarkup.builder()
        .keyboard(List.of(row))
        .resizeKeyboard(true)
        .selective(true)
        .build();
  }

  public InlineKeyboardMarkup buildConnectionInlineKeyboard() {
    InlineKeyboardButton happBtn = InlineKeyboardButton.builder()
        .text("📱 Инструкция: Happ")
        .callbackData(CB_GUIDE_HAPP)
        .build();

    InlineKeyboardButton v2rayTunBtn = InlineKeyboardButton.builder()
        .text("📱 Инструкция: v2rayTun")
        .callbackData(CB_GUIDE_V2RAYTUN)
        .build();

    InlineKeyboardButton vlessKeyBtn = InlineKeyboardButton.builder()
        .text("🔑 Скопировать VLESS-ключ")
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
}
