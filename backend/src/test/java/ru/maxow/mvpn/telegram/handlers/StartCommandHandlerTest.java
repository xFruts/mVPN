package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("StartCommandHandler - unit tests")
class StartCommandHandlerTest {

  private final StartCommandHandler handler = new StartCommandHandler();

  @Test
  @DisplayName("Given /start and first name When handle Then include personalized greeting")
  void givenStartWithFirstNameWhenHandleThenPersonalizedGreeting() {
    Update update = updateWithText(123L, "/start", "Ivan");

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("123");
      assertThat(message.getText()).contains("Привет , Ivan");
      assertThat(message.getText()).contains("/help");
    });
  }

  @Test
  @DisplayName("Given /start and missing first name When handle Then use default greeting")
  void givenStartWithoutFirstNameWhenHandleThenUseDefaultGreeting() {
    Update update = updateWithText(55L, "/start", null);

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message ->
        assertThat(message.getText()).contains("Привет  друг"));
  }

  @Test
  @DisplayName("Given /start text When supports Then return true")
  void givenStartTextWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "/start", "A"))).isTrue();
  }

  @Test
  @DisplayName("Given update without text or non-start command When supports Then return false")
  void givenUnsupportedUpdateWhenSupportsThenFalse() {
    Update noText = mock(Update.class);
    Message noTextMessage = mock(Message.class);
    when(noText.hasMessage()).thenReturn(true);
    when(noText.getMessage()).thenReturn(noTextMessage);
    when(noTextMessage.hasText()).thenReturn(false);

    assertThat(handler.supports(noText)).isFalse();
    assertThat(handler.supports(updateWithText(1L, "/help", "A"))).isFalse();
  }

  private Update updateWithText(Long chatId, String text, String firstName) {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasText()).thenReturn(true);
    when(message.getText()).thenReturn(text);
    when(message.getChatId()).thenReturn(chatId);

    if (firstName == null) {
      when(message.getFrom()).thenReturn(null);
    } else {
      org.telegram.telegrambots.meta.api.objects.User from =
          mock(org.telegram.telegrambots.meta.api.objects.User.class);
      when(from.getFirstName()).thenReturn(firstName);
      when(message.getFrom()).thenReturn(from);
    }

    return update;
  }
}

