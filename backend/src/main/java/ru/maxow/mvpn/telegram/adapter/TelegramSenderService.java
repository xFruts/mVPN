package ru.maxow.mvpn.telegram.adapter;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.maxow.mvpn.telegram.util.TelegramConstants;

/**
 * Сервис отправки сообщений в Telegram через {@link AbsSender}.
 *
 * <p>При {@code bot.enabled=false} (например, в тестах или headless-режиме)
 * бин {@link VpnBot} не создается, и {@code absSender} остается {@code null}.
 * В этом случае вызовы отправки сообщений безопасно пропускаются.
 */
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@Setter
@RequiredArgsConstructor
public class TelegramSenderService {

  AbsSender absSender;

  /**
   * Отправляет текстовое сообщение в указанный чат.
   *
   * @param chatId идентификатор чата
   * @param text   текст сообщения
   */
  public void sendMessage(String chatId, String text) {
    sendMessage(chatId, text, null);
  }

  /**
   * Отправляет сообщение с инлайн-клавиатурой в указанный чат.
   *
   * @param chatId      идентификатор чата
   * @param text        текст сообщения
   * @param replyMarkup разметка инлайн-клавиатуры
   */
  public void sendMessage(String chatId, String text, ReplyKeyboard replyMarkup) {
    if (absSender == null) {
      log.debug("AbsSender не сконфигурирован, пропуск отправки в {}", maskChatId(chatId));
      return;
    }
    SendMessage.SendMessageBuilder builder = SendMessage.builder()
        .chatId(chatId)
        .text(text)
        .parseMode(TelegramConstants.PARSE_MODE_MARKDOWN);
    if (replyMarkup != null) {
      builder.replyMarkup(replyMarkup);
    }
    try {
      absSender.execute(builder.build());
    } catch (TelegramApiException e) {
      if (e.getMessage() != null && e.getMessage().contains("can't parse entities")) {
        try {
          SendMessage plain = SendMessage.builder()
              .chatId(chatId)
              .text(text)
              .replyMarkup(replyMarkup)
              .build();
          absSender.execute(plain);
          return;
        } catch (Exception ignored) {
          // ignore
        }
      }
      log.error("Error while sending Telegram message to {}: {}",
          maskChatId(chatId), e.getMessage());
    }
  }

  private String maskChatId(String chatId) {
    if (chatId == null || chatId.length() <= 4) {
      return "***";
    }
    return chatId.substring(0, 2) + "***" + chatId.substring(chatId.length() - 2);
  }
}
