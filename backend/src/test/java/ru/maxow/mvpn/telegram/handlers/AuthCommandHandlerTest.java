package ru.maxow.mvpn.telegram.handlers;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthCommandHandler - unit tests")
class AuthCommandHandlerTest {

  @Mock
  private UserService userService;

  @InjectMocks
  private AuthCommandHandler handler;

  @Test
  @DisplayName("Given auth command with valid token and unauthenticated user When handle Then link telegram id")
  void givenValidTokenWhenHandleThenAuthenticateUser() {
    UUID token = UUID.randomUUID();
    Update update = updateForHandle(12345L, "/auth " + token);

    when(userService.findByTelegramId(12345L)).thenReturn(null);
    when(userService.checkVerificationCode(token)).thenReturn(true);

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("12345");
      assertThat(message.getText()).contains("успешно аутентифицированы");
    });
    verify(userService).updateUserTelegramId(token, 12345L);
  }

  @Test
  @DisplayName("Given already authenticated telegram id When handle Then return already authenticated message")
  void givenExistingTelegramIdWhenHandleThenSkipTokenValidation() {
    Update update = updateForHandle(777L, "/auth " + UUID.randomUUID());
    User existingUser = new User();

    when(userService.findByTelegramId(777L)).thenReturn(existingUser);

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("777");
      assertThat(message.getText()).contains("уже аутентифицированы");
    });
    verify(userService, never()).checkVerificationCode(org.mockito.ArgumentMatchers.any());
    verify(userService, never()).updateUserTelegramId(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());
  }

  @Test
  @DisplayName("Given auth command without token When handle Then return usage instruction")
  void givenAuthWithoutTokenWhenHandleThenReturnInstruction() {
    Update update = updateForHandle(99L, "/auth");
    when(userService.findByTelegramId(99L)).thenReturn(null);

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message ->
        assertThat(message.getText()).contains("предоставьте токен"));
  }

  @Test
  @DisplayName("Given malformed token When handle Then return invalid format message")
  void givenMalformedTokenWhenHandleThenReturnInvalidFormat() {
    Update update = updateForHandle(1L, "/auth not-a-uuid");
    when(userService.findByTelegramId(1L)).thenReturn(null);

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message ->
        assertThat(message.getText()).contains("Неверный формат токена"));
    verify(userService, never()).checkVerificationCode(org.mockito.ArgumentMatchers.any());
  }

  @Test
  @DisplayName("Given unknown token When handle Then return invalid token message")
  void givenUnknownTokenWhenHandleThenReturnInvalidToken() {
    UUID token = UUID.randomUUID();
    Update update = updateForHandle(101L, "/auth " + token);

    when(userService.findByTelegramId(101L)).thenReturn(null);
    when(userService.checkVerificationCode(token)).thenReturn(false);

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message ->
        assertThat(message.getText()).contains("Неверный токен аутентификации"));
    verify(userService, never()).updateUserTelegramId(token, 101L);
  }

  @Test
  @DisplayName("Given update text starts with /auth When supports Then return true")
  void givenAuthTextWhenSupportsThenTrue() {
    assertThat(handler.supports(updateForSupports("/auth 123"))).isTrue();
  }

  @Test
  @DisplayName("Given update has no message or non-auth text When supports Then return false")
  void givenUnsupportedUpdateWhenSupportsThenFalse() {
    Update noMessage = mock(Update.class);
    when(noMessage.hasMessage()).thenReturn(false);

    Update otherCommand = updateForSupports("/start");

    assertThat(handler.supports(noMessage)).isFalse();
    assertThat(handler.supports(otherCommand)).isFalse();
  }

  private Update updateForHandle(Long chatId, String text) {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    when(update.getMessage()).thenReturn(message);
    when(message.getText()).thenReturn(text);
    when(message.getChatId()).thenReturn(chatId);

    return update;
  }

  private Update updateForSupports(String text) {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasText()).thenReturn(true);
    when(message.getText()).thenReturn(text);

    return update;
  }
}

