package ru.maxow.mvpn.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StartCommandHandlerTest {

    private StartCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new StartCommandHandler();
    }

    private Update createUpdate(String text, String firstName) {
        Update update = new Update();
        Message message = new Message();
        message.setText(text);
        Chat chat = new Chat();
        chat.setId(123L);
        message.setChat(chat);
        User from = new User();
        from.setFirstName(firstName);
        message.setFrom(from);
        update.setMessage(message);
        return update;
    }

    @Test
    void supports_withStartCommand_shouldReturnTrue() {
        Update update = createUpdate("/start", "Test");
        assertTrue(handler.supports(update));
    }

    @Test
    void supports_withOtherCommand_shouldReturnFalse() {
        Update update = createUpdate("/help", "Test");
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
    void handle_withFirstName_shouldReturnPersonalizedMessage() {
        Update update = createUpdate("/start", "John");

        List<SendMessage> result = handler.handle(update);

        assertEquals(1, result.size());
        SendMessage message = result.get(0);
        assertEquals("123", message.getChatId());
        assertTrue(message.getText().contains(", John"));
        assertTrue(message.getText().startsWith("👋Привет"));
    }

    @Test
    void handle_withoutFirstName_shouldReturnGenericMessage() {
        Update update = createUpdate("/start", "DummyName"); // Pass a dummy name to avoid NPE in createUpdate
        update.getMessage().setFrom(null); // Now set the 'from' user to null to test the desired logic

        List<SendMessage> result = handler.handle(update);

        assertEquals(1, result.size());
        SendMessage message = result.get(0);
        assertEquals("123", message.getChatId());
        assertTrue(message.getText().contains(" друг"));
        assertTrue(message.getText().startsWith("👋Привет"));
    }
}
