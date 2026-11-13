package ru.maxow.mvpn.telegram.service;

import java.util.List;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

@Component
public class TelegramKeyboardFactory {

  public static final String BTN_SUBSCRIPTION = "📊 Моя подписка";
  public static final String BTN_SERVERS = "🌐 Доступные сервера";
  public static final String BTN_CONNECTION = "🔗 Подключение";
  public static final String BTN_HELP = "❓ Помощь";

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
}
