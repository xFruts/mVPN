package ru.maxow.mvpn.telegram.adapter;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Telegram-бот сервиса mVPN для приема команд и callback-запросов пользователей.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "bot", name = "enabled", havingValue = "true")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VpnBot extends TelegramLongPollingBot {

  UpdateDispatcher dispatcher;
  String username;

  /**
   * Инициализирует бота и регистрирует его как отправитель в TelegramSenderService.
   *
   * @param botToken токен бота
   * @param username юзернейм бота
   * @param dispatcher диспетчер обновлений
   * @param telegramSenderService сервис отправки сообщений
   */
  public VpnBot(
      @Value("${bot.token}") String botToken,
      @Value("${bot.username}") String username,
      UpdateDispatcher dispatcher,
      TelegramSenderService telegramSenderService
  ) {
    super(botToken);
    this.username = username;
    this.dispatcher = dispatcher;
    telegramSenderService.setAbsSender(this);
  }

  @Override
  public String getBotUsername() {
    return username;
  }

  @Override
  public void onUpdateReceived(Update update) {
    try {
      if (update.hasCallbackQuery()) {
        try {
          execute(AnswerCallbackQuery.builder()
              .callbackQueryId(update.getCallbackQuery().getId())
              .build());
        } catch (Exception e) {
          log.debug("Failed to answer callback query: {}", e.getMessage());
        }
      }
      for (BotApiMethod<?> message : dispatcher.dispatch(update)) {
        try {
          execute(message);
        } catch (TelegramApiException e) {
          if (e.getMessage() != null && e.getMessage().contains("message is not modified")) {
            log.debug("Message content unchanged, skipping edit: {}", e.getMessage());
          } else if (e.getMessage() != null && e.getMessage().contains("can't parse entities")) {
            log.warn("Markdown parse error, attempting fallback without parseMode: {}",
                e.getMessage());
            executeWithoutParseMode(message);
          } else {
            throw e;
          }
        }
      }
    } catch (TelegramApiException e) {
      log.error("Telegram API error: {}", e.getMessage(), e);
    } catch (Exception e) {
      log.error("Unexpected error in onUpdateReceived: {}", e.getMessage(), e);
    }
  }

  /**
   * Повторная отправка сообщения без Markdown parseMode
   * в случае ошибки парсинга сущностей.
   */
  private void executeWithoutParseMode(BotApiMethod<?> message) {
    try {
      if (message instanceof EditMessageText editMsg) {
        EditMessageText plain = EditMessageText.builder()
            .chatId(editMsg.getChatId())
            .messageId(editMsg.getMessageId())
            .inlineMessageId(editMsg.getInlineMessageId())
            .text(editMsg.getText())
            .replyMarkup(editMsg.getReplyMarkup())
            .parseMode(null)
            .build();
        execute(plain);
      } else if (message instanceof SendMessage sendMsg) {
        SendMessage plain = SendMessage.builder()
            .chatId(sendMsg.getChatId())
            .text(sendMsg.getText())
            .replyMarkup(sendMsg.getReplyMarkup())
            .parseMode(null)
            .build();
        execute(plain);
      }
    } catch (Exception ex) {
      log.error("Fallback execution without parseMode failed: {}", ex.getMessage());
    }
  }
}
