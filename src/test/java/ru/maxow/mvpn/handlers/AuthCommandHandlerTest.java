package ru.maxow.mvpn.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthCommandHandlerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthCommandHandler authCommandHandler;

    private Update createMockUpdate(String text, Long chatId) {
        Update update = new Update();
        Message message = new Message();
        Chat chat = new Chat();
        chat.setId(chatId);
        message.setChat(chat);
        message.setText(text);
        update.setMessage(message);
        return update;
    }

    @Test
    void supports_ShouldReturnTrue_ForAuthCommand() {
        // Given
        Update update = createMockUpdate("/auth test", 123L);
        // When
        boolean result = authCommandHandler.supports(update);
        // Then
        assertTrue(result);
    }

    @Test
    void handle_ShouldReturnAlreadyAuthenticated_WhenUserExists() {
        // Given
        Long chatId = 123L;
        Update update = createMockUpdate("/auth some-token", chatId);
        when(userService.findByTelegramId(chatId)).thenReturn(new User());

        // When
        List<SendMessage> result = authCommandHandler.handle(update);

        // Then
        assertEquals(1, result.size());
        assertEquals("✅ Вы уже аутентифицированы.", result.get(0).getText());
        verify(userService, never()).checkVerificationCode(any());
    }

    @Test
    void handle_ShouldReturnSuccess_WhenTokenIsValid() {
        // Given
        Long chatId = 123L;
        UUID token = UUID.randomUUID();
        Update update = createMockUpdate("/auth " + token, chatId);
        when(userService.findByTelegramId(chatId)).thenReturn(null);
        when(userService.checkVerificationCode(token)).thenReturn(true);

        // When
        List<SendMessage> result = authCommandHandler.handle(update);

        // Then
        assertEquals(1, result.size());
        assertEquals("✅ Вы успешно аутентифицированы!", result.get(0).getText());
        verify(userService, times(1)).updateUserTelegramId(token, chatId);
    }

    @Test
    void handle_ShouldReturnInvalidToken_WhenTokenIsWrong() {
        // Given
        Long chatId = 123L;
        UUID token = UUID.randomUUID();
        Update update = createMockUpdate("/auth " + token, chatId);
        when(userService.findByTelegramId(chatId)).thenReturn(null);
        when(userService.checkVerificationCode(token)).thenReturn(false);

        // When
        List<SendMessage> result = authCommandHandler.handle(update);

        // Then
        assertEquals(1, result.size());
        assertEquals("❌ Неверный токен аутентификации. Пожалуйста, попробуйте снова.", result.get(0).getText());
        verify(userService, never()).updateUserTelegramId(any(), any());
    }

    @Test
    void handle_ShouldReturnInvalidFormat_WhenTokenIsMalformed() {
        // Given
        Long chatId = 123L;
        Update update = createMockUpdate("/auth not-a-uuid", chatId);
        when(userService.findByTelegramId(chatId)).thenReturn(null);

        // When
        List<SendMessage> result = authCommandHandler.handle(update);

        // Then
        assertEquals(1, result.size());
        assertEquals("❌ Неверный формат токена. Пожалуйста, проверьте правильность ввода.", result.get(0).getText());
    }

    @Test
    void handle_ShouldReturnProvideToken_WhenTokenIsMissing() {
        // Given
        Long chatId = 123L;
        Update update = createMockUpdate("/auth", chatId);
        when(userService.findByTelegramId(chatId)).thenReturn(null);

        // When
        List<SendMessage> result = authCommandHandler.handle(update);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getText().contains("Пожалуйста, предоставьте токен аутентификации"));
    }
}

