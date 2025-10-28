package ru.maxow.mvpn.telegram.adapter;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Service for sending messages with absSender via Telegram bot.
 */
@Service
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
@Setter
@RequiredArgsConstructor
public class TelegramSenderService {
  AbsSender absSender;

  /**
   * Send message to chat.
   *
   * @param chatId chat id
   * @param text   message text
   */
  public void sendMessage(String chatId, String text) {
    SendMessage sendMessage = SendMessage.builder()
        .chatId(chatId)
        .text(text)
        .build();
    try {
      absSender.execute(sendMessage);
    } catch (TelegramApiException e) {
      log.error("Error while send scheduled message: {}", e.getMessage());
    }
  }
}
