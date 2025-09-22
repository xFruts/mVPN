package ru.maxow.mvpn.adapter.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VPNBotTest {

    @Mock
    private UpdateDispatcher dispatcher;

    private VPNBot bot;

    @BeforeEach
    void setUp() {
        // We spy on the bot to be able to mock the 'execute' method and avoid real API calls.
        VPNBot realBot = new VPNBot("test-token", "test-username", dispatcher);
        bot = spy(realBot);
    }

    @Test
    void onUpdateReceived_shouldDispatchUpdateAndExecuteMessages() throws TelegramApiException {
        Update update = new Update();
        SendMessage message = SendMessage.builder().chatId("123").text("hello").build();
        when(dispatcher.dispatch(update)).thenReturn(List.of(message));
        doReturn(null).when(bot).execute(any(SendMessage.class));

        bot.onUpdateReceived(update);

        verify(dispatcher).dispatch(update);
        verify(bot).execute(message);
    }

    @Test
    void onUpdateReceived_whenDispatcherReturnsEmptyList_shouldNotExecute() throws TelegramApiException {
        Update update = new Update();
        when(dispatcher.dispatch(update)).thenReturn(Collections.emptyList());

        bot.onUpdateReceived(update);

        verify(dispatcher).dispatch(update);
        verify(bot, never()).execute(any(SendMessage.class));
    }

    @Test
    void onUpdateReceived_whenExecuteThrowsTelegramApiException_shouldCatchAndLogError() throws TelegramApiException {
        Update update = new Update();
        SendMessage message = SendMessage.builder().chatId("123").text("hello").build();
        when(dispatcher.dispatch(update)).thenReturn(List.of(message));
        doThrow(new TelegramApiException("API Error")).when(bot).execute(message);

        // The test will pass if no exception is thrown, confirming it was caught.
        bot.onUpdateReceived(update);

        verify(dispatcher).dispatch(update);
        verify(bot).execute(message);
    }

    @Test
    void onUpdateReceived_whenDispatcherThrowsException_shouldCatchAndLogError() throws TelegramApiException {
        Update update = new Update();
        when(dispatcher.dispatch(update)).thenThrow(new RuntimeException("Dispatcher error"));

        // The test will pass if no exception is thrown, confirming it was caught.
        bot.onUpdateReceived(update);

        verify(dispatcher).dispatch(update);
        verify(bot, never()).execute(any(SendMessage.class));
    }
}

