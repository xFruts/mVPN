package ru.maxow.mvpn.telegram.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.user.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultCommandHandler - unit tests")
class DefaultCommandHandlerTest {

  @Mock
  private TelegramUserService userService;

  private final TelegramMessageFormatter messageFormatter = new TelegramMessageFormatter();
  private final TelegramKeyboardFactory keyboardFactory = new TelegramKeyboardFactory();

  private DefaultCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new DefaultCommandHandler(userService, messageFormatter, keyboardFactory);
  }

  @Test
  @DisplayName("Given unknown command When handle Then return fallback message")
  void givenUnknownCommandWhenHandleThenFallbackMessage() {
    Update update = updateWithText(42L, "/unknown");
    when(userService.findAuthenticatedUser(eq(42L), any())).thenReturn(Optional.of(new User()));

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("42");
      assertThat(message.getText()).contains("Неизвестная команда");
      assertThat(message.getText()).contains("/help");
      assertThat(message.getReplyMarkup()).isNotNull();
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

    lenient().when(update.hasMessage()).thenReturn(true);
    lenient().when(update.getMessage()).thenReturn(message);
    lenient().when(message.hasText()).thenReturn(true);
    lenient().when(message.getText()).thenReturn(text);
    lenient().when(message.getChatId()).thenReturn(chatId);

    return update;
  }
}
