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

@DisplayName("DefaultCommandHandler - unit tests")
class DefaultCommandHandlerTest {

  private final DefaultCommandHandler handler = new DefaultCommandHandler();

  @Test
  @DisplayName("Given unknown command When handle Then return fallback message")
  void givenUnknownCommandWhenHandleThenFallbackMessage() {
    Update update = updateWithText(42L, "/unknown");

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("42");
      assertThat(message.getText()).contains("Неизвестная команда");
      assertThat(message.getText()).contains("/help");
    });
  }

  @Test
  @DisplayName("Given update has message When supports Then return true")
  void givenMessageWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "hello"))).isTrue();
  }

  @Test
  @DisplayName("Given update without message When supports Then return false")
  void givenNoMessageWhenSupportsThenFalse() {
    Update update = mock(Update.class);
    when(update.hasMessage()).thenReturn(false);

    assertThat(handler.supports(update)).isFalse();
  }

  private Update updateWithText(Long chatId, String text) {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasText()).thenReturn(true);
    when(message.getText()).thenReturn(text);
    when(message.getChatId()).thenReturn(chatId);

    return update;
  }
}

