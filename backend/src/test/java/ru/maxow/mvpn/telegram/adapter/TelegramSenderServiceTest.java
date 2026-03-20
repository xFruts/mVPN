package ru.maxow.mvpn.telegram.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramSenderService - Unit тесты (бизнес-логика)")
class TelegramSenderServiceTest {

  @Mock
  private AbsSender absSender;

  @InjectMocks
  private TelegramSenderService telegramSenderService;

  @BeforeEach
  void setUp() {
    telegramSenderService.setAbsSender(absSender);
  }

  @Nested
  @DisplayName("Отправка сообщений в Telegram")
  class SendMessageTests {

    @Test
    @DisplayName("Должен успешно отправить сообщение в чат")
    void shouldSendMessageSuccessfully() throws TelegramApiException {
      String chatId = "123456789";
      String messageText = "Здравствуйте! Это тестовое сообщение.";

      when(absSender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));

      assertThatNoException().isThrownBy(() -> telegramSenderService.sendMessage(chatId, messageText));

      ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
      verify(absSender).execute(messageCaptor.capture());

      SendMessage capturedMessage = messageCaptor.getValue();
      assertThat(capturedMessage.getChatId()).isEqualTo(chatId);
      assertThat(capturedMessage.getText()).isEqualTo(messageText);
    }

    @Test
    @DisplayName("Должен корректно формировать SendMessage с правильными параметрами")
    void shouldCreateCorrectSendMessageObject() throws TelegramApiException {
      String chatId = "987654321";
      String text = "Сообщение с юникодом: привет, мир! 🚀";

      when(absSender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));

      telegramSenderService.sendMessage(chatId, text);

      ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
      verify(absSender).execute(messageCaptor.capture());

      SendMessage message = messageCaptor.getValue();
      assertThat(message.getChatId()).isEqualTo(chatId);
      assertThat(message.getText()).isEqualTo(text);
    }

    @Test
    @DisplayName("Должен отправить сообщение с пустым текстом")
    void shouldSendMessageWithEmptyText() throws TelegramApiException {
      String chatId = "123456789";
      String emptyText = "";

      when(absSender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));

      telegramSenderService.sendMessage(chatId, emptyText);

      ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
      verify(absSender).execute(messageCaptor.capture());

      assertThat(messageCaptor.getValue().getText()).isEmpty();
    }

    @Test
    @DisplayName("Должен отправить длинное сообщение")
    void shouldSendLongMessage() throws TelegramApiException {
      String chatId = "123456789";
      String longText = "A".repeat(1000);

      when(absSender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));

      telegramSenderService.sendMessage(chatId, longText);

      verify(absSender).execute(any(SendMessage.class));
    }

    @Test
    @DisplayName("Должен отправить сообщение в разные чаты")
    void shouldSendMessagesToDifferentChats() throws TelegramApiException {
      String chatId1 = "111111111";
      String chatId2 = "222222222";
      String message = "Test message";

      when(absSender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));

      telegramSenderService.sendMessage(chatId1, message);
      telegramSenderService.sendMessage(chatId2, message);

      ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
      verify(absSender, times(2)).execute(messageCaptor.capture());

      var allMessages = messageCaptor.getAllValues();
      assertThat(allMessages).hasSize(2);
      assertThat(allMessages.get(0).getChatId()).isEqualTo(chatId1);
      assertThat(allMessages.get(1).getChatId()).isEqualTo(chatId2);
    }

    @Test
    @DisplayName("Должен отправить несколько сообщений в один чат")
    void shouldSendMultipleMessagesToSameChat() throws TelegramApiException {
      String chatId = "123456789";
      String message1 = "First message";
      String message2 = "Second message";
      String message3 = "Third message";

      when(absSender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));

      telegramSenderService.sendMessage(chatId, message1);
      telegramSenderService.sendMessage(chatId, message2);
      telegramSenderService.sendMessage(chatId, message3);

      ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
      verify(absSender, times(3)).execute(messageCaptor.capture());

      var allMessages = messageCaptor.getAllValues();
      assertThat(allMessages).hasSize(3);
      assertThat(allMessages.stream().map(SendMessage::getText))
          .containsExactly(message1, message2, message3);
    }
  }

  @Nested
  @DisplayName("Обработка ошибок Telegram API")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Должен не выбросить исключение при ошибке Telegram API (логирование ошибки)")
    void shouldHandleTelegramApiExceptionGracefully() throws TelegramApiException {
      String chatId = "123456789";
      String messageText = "Test message";
      TelegramApiException telegramException = new TelegramApiException("Bot was blocked by the user");

      doThrow(telegramException).when(absSender).execute(any(SendMessage.class));

      assertThatNoException().isThrownBy(() -> telegramSenderService.sendMessage(chatId, messageText));

      verify(absSender).execute(any(SendMessage.class));
    }

    @Test
    @DisplayName("Должен обработать исключение 'Chat not found'")
    void shouldHandleChatNotFoundError() throws TelegramApiException {
      String invalidChatId = "999999999";
      String messageText = "Message to non-existent chat";
      TelegramApiException chatNotFoundException = new TelegramApiException("Chat not found");

      doThrow(chatNotFoundException).when(absSender).execute(any(SendMessage.class));

      assertThatNoException().isThrownBy(() -> telegramSenderService.sendMessage(invalidChatId, messageText));

      verify(absSender).execute(any(SendMessage.class));
    }

    @Test
    @DisplayName("Должен обработать исключение при сетевой ошибке")
    void shouldHandleNetworkException() throws TelegramApiException {
      String chatId = "123456789";
      String messageText = "Test during network error";
      TelegramApiException networkException = new TelegramApiException("Connection timeout");

      doThrow(networkException).when(absSender).execute(any(SendMessage.class));

      assertThatNoException().isThrownBy(() -> telegramSenderService.sendMessage(chatId, messageText));

      verify(absSender).execute(any(SendMessage.class));
    }

    @Test
    @DisplayName("Должен продолжить работу после ошибки и отправить следующее сообщение")
    void shouldContinueWorkingAfterException() throws TelegramApiException {
      String chatId = "123456789";
      String message1 = "First message";
      String message2 = "Second message";

      TelegramApiException exception = new TelegramApiException("Temporary error");
      when(absSender.execute(any(SendMessage.class)))
          .thenThrow(exception)
          .thenReturn(mock(Message.class));

      telegramSenderService.sendMessage(chatId, message1);
      telegramSenderService.sendMessage(chatId, message2);

      ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
      verify(absSender, times(2)).execute(messageCaptor.capture());
    }
  }

  @Nested
  @DisplayName("Специальные символы и форматирование")
  class SpecialCharactersAndFormattingTests {

    @Test
    @DisplayName("Должен отправить сообщение с HTML тегами")
    void shouldSendMessageWithHtmlTags() throws TelegramApiException {
      String chatId = "123456789";
      String messageWithHtml = "<b>Жирный текст</b> и <i>курсив</i>";

      when(absSender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));

      telegramSenderService.sendMessage(chatId, messageWithHtml);

      ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
      verify(absSender).execute(messageCaptor.capture());

      assertThat(messageCaptor.getValue().getText()).contains("<b>", "</b>", "<i>", "</i>");
    }

    @Test
    @DisplayName("Должен отправить сообщение с переносами строк")
    void shouldSendMessageWithLineBreaks() throws TelegramApiException {
      String chatId = "123456789";
      String messageWithLineBreaks = "Строка 1\nСтрока 2\nСтрока 3";

      when(absSender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));

      telegramSenderService.sendMessage(chatId, messageWithLineBreaks);

      ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
      verify(absSender).execute(messageCaptor.capture());

      assertThat(messageCaptor.getValue().getText()).contains("\n");
    }

    @Test
    @DisplayName("Должен отправить сообщение с кириллицей")
    void shouldSendMessageWithCyrillicCharacters() throws TelegramApiException {
      String chatId = "123456789";
      String russianMessage = "Привет! Это сообщение на русском языке. Как дела?";

      when(absSender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));

      telegramSenderService.sendMessage(chatId, russianMessage);

      ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
      verify(absSender).execute(messageCaptor.capture());

      assertThat(messageCaptor.getValue().getText()).isEqualTo(russianMessage);
    }

    @Test
    @DisplayName("Должен отправить сообщение с эмодзи")
    void shouldSendMessageWithEmojis() throws TelegramApiException {
      String chatId = "123456789";
      String messageWithEmojis = "🎉 Успех! ✅ Операция завершена. 🚀";

      when(absSender.execute(any(SendMessage.class))).thenReturn(mock(Message.class));

      telegramSenderService.sendMessage(chatId, messageWithEmojis);

      ArgumentCaptor<SendMessage> messageCaptor = ArgumentCaptor.forClass(SendMessage.class);
      verify(absSender).execute(messageCaptor.capture());

      assertThat(messageCaptor.getValue().getText()).isEqualTo(messageWithEmojis);
    }
  }
}

