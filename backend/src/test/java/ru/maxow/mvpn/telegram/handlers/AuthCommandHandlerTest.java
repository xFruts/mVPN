package ru.maxow.mvpn.telegram.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthCommandHandler - unit tests")
class AuthCommandHandlerTest {

  @Mock
  private UserService userService;

  @Spy
  private TelegramMessageFormatter messageFormatter = new TelegramMessageFormatter();

  @Spy
  private TelegramKeyboardFactory keyboardFactory = new TelegramKeyboardFactory();

  @InjectMocks
  private AuthCommandHandler handler;

  @Test
  @DisplayName("Given valid token and unauthenticated user When handle Then link telegram id")
  void givenValidTokenWhenHandleThenAuthenticateUser() {
    UUID token = UUID.randomUUID();
    Update update = updateWithText(12345L, "/auth " + token);

    when(userService.findByTelegramId(12345L)).thenReturn(null);
    when(userService.checkVerificationCode(token)).thenReturn(true);

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(method -> {
      assertThat(method).isInstanceOf(SendMessage.class);
      SendMessage message = (SendMessage) method;
      assertThat(message.getChatId()).isEqualTo("12345");
      assertThat(message.getText()).contains("успешно аутентифицированы");
    });
    verify(userService).updateUserTelegramId(token, 12345L);
  }

  @Test
  @DisplayName("Given already authenticated user When handle Then return rejection message")
  void givenExistingTelegramIdWhenHandleThenSkipTokenValidation() {
    Update update = updateWithText(777L, "/auth " + UUID.randomUUID());
    User existingUser = new User();

    when(userService.findByTelegramId(777L)).thenReturn(existingUser);

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(method -> {
      assertThat(method).isInstanceOf(SendMessage.class);
      SendMessage message = (SendMessage) method;
      assertThat(message.getChatId()).isEqualTo("777");
      assertThat(message.getText()).contains("уже аутентифицированы");
    });
    verify(userService, never()).checkVerificationCode(any());
  }

  @Test
  @DisplayName("Given auth command without token When handle Then prompt user")
  void givenMissingTokenWhenHandleThenPrompt() {
    Update update = updateWithText(10L, "/auth");
    when(userService.findByTelegramId(10L)).thenReturn(null);

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(method -> {
      assertThat(method).isInstanceOf(SendMessage.class);
      SendMessage message = (SendMessage) method;
      assertThat(message.getText()).contains("предоставьте токен аутентификации");
    });
  }

  @Test
  @DisplayName("Given auth command with invalid UUID When handle Then return format error")
  void givenInvalidUuidWhenHandleThenReturnFormatError() {
    Update update = updateWithText(10L, "/auth not-a-uuid");
    when(userService.findByTelegramId(10L)).thenReturn(null);

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(method -> {
      assertThat(method).isInstanceOf(SendMessage.class);
      SendMessage message = (SendMessage) method;
      assertThat(message.getText()).contains("Неверный формат токена");
    });
  }

  @Test
  @DisplayName("Given auth command with unknown token When handle Then return rejection")
  void givenUnknownTokenWhenHandleThenReject() {
    UUID token = UUID.randomUUID();
    Update update = updateWithText(10L, "/auth " + token);
    when(userService.findByTelegramId(10L)).thenReturn(null);
    when(userService.checkVerificationCode(token)).thenReturn(false);

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(method -> {
      assertThat(method).isInstanceOf(SendMessage.class);
      SendMessage message = (SendMessage) method;
      assertThat(message.getText()).contains("Неверный токен аутентификации");
    });
    verify(userService, never()).updateUserTelegramId(any(), anyLong());
  }

  @Test
  @DisplayName("Given /auth text When supports Then return true")
  void givenAuthTextWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "/auth abc"))).isTrue();
  }

  @Test
  @DisplayName("Given non-auth text When supports Then return false")
  void givenNonAuthTextWhenSupportsThenFalse() {
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
