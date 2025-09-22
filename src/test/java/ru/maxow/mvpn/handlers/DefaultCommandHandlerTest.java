package ru.maxow.mvpn.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultCommandHandlerTest {

    private DefaultCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DefaultCommandHandler();
    }

    private Update createUpdate(String text) {
        Update update = new Update();
        Message message = new Message();
        message.setText(text);
        Chat chat = new Chat();
        chat.setId(123L);
        message.setChat(chat);
        update.setMessage(message);
        return update;
    }

    @Test
    void supports_withAnyMessage_shouldReturnTrue() {
        Update update = createUpdate("any text");
        assertTrue(handler.supports(update));
    }

    @Test
    void supports_withNoMessage_shouldReturnFalse() {
        Update update = new Update();
        assertFalse(handler.supports(update));
    }

    @Test
    void handle_shouldReturnUnknownCommandMessage() {
        Update update = createUpdate("some random text");

        List<SendMessage> result = handler.handle(update);

        assertEquals(1, result.size());
        SendMessage message = result.get(0);
        assertEquals("123", message.getChatId());
        assertEquals("❓Неизвестная команда. Посмотреть список доступных команд: /help", message.getText());
    }
}

