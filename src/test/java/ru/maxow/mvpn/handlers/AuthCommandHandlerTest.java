package ru.maxow.mvpn.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.user.UserService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthCommandHandlerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthCommandHandler authCommandHandler;

    private Update update;
    private Message message;

    @BeforeEach
    void setUp() {
        update = new Update();
        message = new Message();
        Chat chat = new Chat();
        chat.setId(123L);
        message.setChat(chat);
        update.setMessage(message);
    }

    @Test
    void supports_shouldReturnTrue_whenCommandIsAuth() {
        message.setText("/auth some_token");
        assertTrue(authCommandHandler.supports(update));
    }

    @Test
    void supports_shouldReturnFalse_whenCommandIsNotAuth() {
        message.setText("/start");
        assertFalse(authCommandHandler.supports(update));
    }

    @Test
    void supports_shouldReturnFalse_whenMessageHasNoText() {
        assertFalse(authCommandHandler.supports(update));
    }

    @Test
    void handle_shouldReturnHelpMessage_whenNoTokenProvided() {
        message.setText("/auth");

        List<SendMessage> result = authCommandHandler.handle(update);

        assertEquals(1, result.size());
        assertEquals("❌ Пожалуйста, предоставьте токен аутентификации после команды /auth. Например: /auth your_token_here", result.get(0).getText());
    }

    @Test
    void handle_shouldReturnInvalidFormatMessage_whenTokenIsInvalidUUID() {
        message.setText("/auth invalid-uuid");

        List<SendMessage> result = authCommandHandler.handle(update);

        assertEquals(1, result.size());
        assertEquals("❌ Неверный формат токена. Пожалуйста, проверьте правильность ввода.", result.get(0).getText());
    }

    @Test
    void handle_shouldReturnWrongTokenMessage_whenTokenIsIncorrect() {
        UUID token = UUID.randomUUID();
        message.setText("/auth " + token);
        when(userService.checkVerificationCode(token)).thenReturn(false);

        List<SendMessage> result = authCommandHandler.handle(update);

        assertEquals(1, result.size());
        assertEquals("❌ Неверный токен аутентификации. Пожалуйста, попробуйте снова.", result.get(0).getText());
        verify(userService, never()).updateUserTelegramId(any(), any());
    }

    @Test
    void handle_shouldReturnSuccessMessage_whenTokenIsCorrect() {
        UUID token = UUID.randomUUID();
        long chatId = 123L;
        message.setText("/auth " + token);
        when(userService.checkVerificationCode(token)).thenReturn(true);

        List<SendMessage> result = authCommandHandler.handle(update);

        assertEquals(1, result.size());
        assertEquals("✅ Вы успешно аутентифицированы!", result.get(0).getText());
        verify(userService, times(1)).updateUserTelegramId(token, chatId);
    }
}

