package ru.maxow.mvpn.adapter.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.handlers.DefaultCommandHandler;
import ru.maxow.mvpn.handlers.HelpCommandHandler;
import ru.maxow.mvpn.handlers.StartCommandHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class UpdateDispatcherTest {

    private UpdateDispatcher updateDispatcher;
    private StartCommandHandler startCommandHandler;
    private HelpCommandHandler helpCommandHandler;
    private DefaultCommandHandler defaultCommandHandler;

    @BeforeEach
    void setUp() {
        // Using real handlers to test integration and logic, spying to verify calls
        startCommandHandler = spy(new StartCommandHandler());
        helpCommandHandler = spy(new HelpCommandHandler());
        defaultCommandHandler = spy(new DefaultCommandHandler());

        // The order is important, reflecting Spring's @Order annotation
        List<CommandHandler> handlers = List.of(startCommandHandler, helpCommandHandler, defaultCommandHandler);
        updateDispatcher = new UpdateDispatcher(handlers);
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
    void dispatch_whenStartCommand_shouldCallStartHandler() {
        Update update = createUpdate("/start");

        List<SendMessage> result = updateDispatcher.dispatch(update);

        verify(startCommandHandler).supports(update);
        verify(startCommandHandler).handle(update);
        verify(helpCommandHandler, never()).handle(update);
        verify(defaultCommandHandler, never()).handle(update);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().getText().contains("Привет"));
    }

    @Test
    void dispatch_whenHelpCommand_shouldCallHelpHandler() {
        Update update = createUpdate("/help");

        List<SendMessage> result = updateDispatcher.dispatch(update);

        verify(helpCommandHandler).supports(update);
        verify(helpCommandHandler).handle(update);
        verify(startCommandHandler, atMost(1)).supports(update);
        verify(startCommandHandler, never()).handle(update);
        verify(defaultCommandHandler, never()).handle(update);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().getText().contains("Список доступных команд"));
    }

    @Test
    void dispatch_whenUnknownCommand_shouldCallDefaultHandler() {
        Update update = createUpdate("some random text");

        List<SendMessage> result = updateDispatcher.dispatch(update);

        verify(defaultCommandHandler).supports(update);
        verify(defaultCommandHandler).handle(update);
        verify(startCommandHandler, atMost(1)).supports(update);
        verify(helpCommandHandler, atMost(1)).supports(update);
        verify(startCommandHandler, never()).handle(update);
        verify(helpCommandHandler, never()).handle(update);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().getText().contains("Неизвестная команда"));
    }

    @Test
    void dispatch_whenNoSupportingHandler_shouldReturnEmptyList() {
        updateDispatcher = new UpdateDispatcher(List.of()); // No handlers
        Update update = createUpdate("/start");

        List<SendMessage> result = updateDispatcher.dispatch(update);

        assertTrue(result.isEmpty());
    }
}

