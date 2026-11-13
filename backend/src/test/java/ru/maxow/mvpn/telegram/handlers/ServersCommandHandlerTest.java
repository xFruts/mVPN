package ru.maxow.mvpn.telegram.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.user.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ServersCommandHandler - unit tests")
class ServersCommandHandlerTest {

  @Mock
  private TelegramUserService userService;

  private final TelegramMessageFormatter messageFormatter = new TelegramMessageFormatter();
  private final TelegramKeyboardFactory keyboardFactory = new TelegramKeyboardFactory();

  private ServersCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ServersCommandHandler(userService, messageFormatter, keyboardFactory);
  }

  @Test
  @DisplayName("Given unauthenticated user When handle Then return prompt to /auth")
  void givenUnauthenticatedUserWhenHandleThenReturnAuthPrompt() {
    Update update = updateWithText(100L, "/servers");
    when(userService.findAuthenticatedUser(eq(100L), any())).thenReturn(Optional.empty());

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(SendMessage.class);
    SendMessage message = (SendMessage) result.get(0);
    assertThat(message.getChatId()).isEqualTo("100");
    assertThat(message.getText()).contains("Вы ещё не авторизованы");
  }

  @Test
  @DisplayName("Given authenticated user When handle Then return safe servers")
  void givenAuthenticatedUserWhenHandleThenReturnSafeServerList() {
    Update update = updateWithText(200L, TelegramKeyboardFactory.BTN_SERVERS);
    User user = new User();
    user.setId(5L);

    Server server1 = new Server();
    server1.setName("Финляндия");
    server1.setLocation("FI");
    server1.setStatus(ServerStatus.ACTIVE);
    server1.setCountryEmoji("🇫🇮");

    Server server2 = new Server();
    server2.setName("Германия");
    server2.setLocation("DE");
    server2.setStatus(ServerStatus.MAINTENANCE);
    server2.setCountryEmoji("🇩🇪");

    when(userService.findAuthenticatedUser(eq(200L), any())).thenReturn(Optional.of(user));
    when(userService.getAvailableServers(user)).thenReturn(List.of(server1, server2));

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(SendMessage.class);
    SendMessage message = (SendMessage) result.get(0);
    assertThat(message.getChatId()).isEqualTo("200");
    assertThat(message.getText()).contains("Финляндия");
    assertThat(message.getText()).contains("Германия");
    assertThat(message.getText()).contains("Работает стабильно");
    assertThat(message.getText()).contains("Технические работы");
    assertThat(message.getReplyMarkup()).isNotNull();
  }

  @Test
  @DisplayName("Given empty server list When handle Then return friendly message")
  void givenEmptyServerListWhenHandleThenReturnEmptyMessage() {
    Update update = updateWithText(200L, "/servers");
    User user = new User();
    user.setId(5L);

    when(userService.findAuthenticatedUser(eq(200L), any())).thenReturn(Optional.of(user));
    when(userService.getAvailableServers(user)).thenReturn(List.of());

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(SendMessage.class);
    SendMessage message = (SendMessage) result.get(0);
    assertThat(message.getText()).contains("список доступных серверов пуст");
  }

  @Test
  @DisplayName("Given supported texts When supports Then return true")
  void givenSupportedTextsWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "/servers"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, TelegramKeyboardFactory.BTN_SERVERS))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/other"))).isFalse();
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
