package ru.maxow.mvpn.telegram.handlers;

import static org.assertj.core.api.Assertions.assertThat;
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
@DisplayName("HelpCommandHandler - unit tests")
class HelpCommandHandlerTest {

  @Mock
  private TelegramUserService userService;

  private final TelegramMessageFormatter messageFormatter = new TelegramMessageFormatter();
  private final TelegramKeyboardFactory keyboardFactory = new TelegramKeyboardFactory();

  private HelpCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new HelpCommandHandler(userService, messageFormatter, keyboardFactory);
  }

  @Test
  @DisplayName("Given /help for authenticated user When handle Then return full command list")
  void givenHelpCommandWhenAuthenticatedThenReturnFullCommandList() {
    Update update = updateWithText(500L, "/help");
    when(userService.findAuthenticatedUser(500L)).thenReturn(Optional.of(new User()));

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("500");
      assertThat(message.getText()).contains("/status");
      assertThat(message.getText()).contains("/servers");
      assertThat(message.getText()).contains("/connect");
    });
  }

  @Test
  @DisplayName("Given /help for unauthenticated user When handle Then return auth instructions")
  void givenHelpCommandWhenUnauthenticatedThenReturnAuthHelp() {
    Update update = updateWithText(500L, "/help");
    when(userService.findAuthenticatedUser(500L)).thenReturn(Optional.empty());

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("500");
      assertThat(message.getText()).contains("/auth");
    });
  }

  @Test
  @DisplayName("Given /help text or button text When supports Then return true")
  void givenHelpTextWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "/help"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, TelegramKeyboardFactory.BTN_HELP))).isTrue();
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

    lenient().when(update.hasMessage()).thenReturn(true);
    lenient().when(update.getMessage()).thenReturn(message);
    lenient().when(message.hasText()).thenReturn(true);
    lenient().when(message.getText()).thenReturn(text);
    lenient().when(message.getChatId()).thenReturn(chatId);

    return update;
  }
}
