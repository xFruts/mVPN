package ru.maxow.mvpn.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HelpCommandHandlerTest {

    private HelpCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HelpCommandHandler();
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
    void supports_withHelpCommand_shouldReturnTrue() {
        Update update = createUpdate("/help");
        assertTrue(handler.supports(update));
    }

    @Test
    void supports_withOtherCommand_shouldReturnFalse() {
        Update update = createUpdate("/start");
        assertFalse(handler.supports(update));
    }

    @Test
    void supports_withNoText_shouldReturnFalse() {
        Update update = new Update();
        Message message = new Message();
        update.setMessage(message);
        assertFalse(handler.supports(update));
    }

    @Test
    void handle_shouldReturnHelpMessage() {
        Update update = createUpdate("/help");

        List<SendMessage> result = handler.handle(update);

        assertEquals(1, result.size());
        SendMessage message = result.get(0);
        assertEquals("123", message.getChatId());
        assertTrue(message.getText().contains("Список доступных команд"));
        assertTrue(message.getText().contains("/start"));
        assertTrue(message.getText().contains("/auth"));
    }
}

