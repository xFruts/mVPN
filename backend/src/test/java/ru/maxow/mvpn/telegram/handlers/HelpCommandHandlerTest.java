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

@DisplayName("HelpCommandHandler - unit tests")
class HelpCommandHandlerTest {

  private final HelpCommandHandler handler = new HelpCommandHandler();

  @Test
  @DisplayName("Given /help command When handle Then return help text with auth command")
  void givenHelpCommandWhenHandleThenReturnHelpText() {
    Update update = updateWithText(500L, "/help");

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("500");
      assertThat(message.getText()).contains("предоставления доступа к VPN-сервису");
      assertThat(message.getText()).contains("/auth");
    });
  }

  @Test
  @DisplayName("Given /help text When supports Then return true")
  void givenHelpTextWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "/help"))).isTrue();
  }

  @Test
  @DisplayName("Given non-help update When supports Then return false")
  void givenNonHelpUpdateWhenSupportsThenFalse() {
    Update noMessage = mock(Update.class);
    when(noMessage.hasMessage()).thenReturn(false);

    assertThat(handler.supports(noMessage)).isFalse();
    assertThat(handler.supports(updateWithText(1L, "/start"))).isFalse();
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

