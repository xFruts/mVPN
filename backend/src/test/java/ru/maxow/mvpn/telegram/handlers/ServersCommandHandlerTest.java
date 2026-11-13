package ru.maxow.mvpn.telegram.handlers;

import static org.assertj.core.api.Assertions.assertThat;
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
    when(userService.findAuthenticatedUser(100L)).thenReturn(Optional.empty());

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("100");
      assertThat(message.getText()).contains("Вы ещё не авторизованы");
    });
  }

  @Test
  @DisplayName("Given authenticated user When handle Then return servers without sensitive IP data")
  void givenAuthenticatedUserWhenHandleThenReturnSafeServerList() {
    Update update = updateWithText(200L, TelegramKeyboardFactory.BTN_SERVERS);
    User user = new User();
    user.setId(5L);

    Server server1 = new Server();
    server1.setName("Финляндия");
    server1.setLocation("Helsinki");
    server1.setCountryEmoji("🇫🇮");
    server1.setStatus(ServerStatus.ACTIVE);
    server1.setIp("192.168.1.100");

    Server server2 = new Server();
    server2.setName("Германия");
    server2.setLocation("Frankfurt");
    server2.setCountryEmoji("🇩🇪");
    server2.setStatus(ServerStatus.MAINTENANCE);
    server2.setIp("10.0.0.5");

    when(userService.findAuthenticatedUser(200L)).thenReturn(Optional.of(user));
    when(userService.getAvailableServers(user)).thenReturn(List.of(server1, server2));

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("200");
      assertThat(message.getText()).contains("Финляндия (Helsinki)");
      assertThat(message.getText()).contains("🟢 Работает стабильно");
      assertThat(message.getText()).contains("Германия (Frankfurt)");
      assertThat(message.getText()).contains("🟡 Технические работы");
      assertThat(message.getText()).contains("Если сервер со статусом 🟢 у вас не работает");

      // Verify ZERO technical leaks
      assertThat(message.getText()).doesNotContain("192.168.1.100");
      assertThat(message.getText()).doesNotContain("10.0.0.5");
      assertThat(message.getReplyMarkup()).isNotNull();
    });
  }

  @Test
  @DisplayName("Given authenticated user without servers When handle Then return empty message")
  void givenUserWithoutServersWhenHandleThenReturnEmptyMessage() {
    Update update = updateWithText(100L, "/servers");
    User user = new User();
    user.setId(5L);

    when(userService.findAuthenticatedUser(100L)).thenReturn(Optional.of(user));
    when(userService.getAvailableServers(user)).thenReturn(List.of());

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("100");
      assertThat(message.getText()).contains("список доступных серверов пуст");
    });
  }

  @Test
  @DisplayName("Given supported texts When supports Then return true")
  void givenSupportedTextsWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "/servers"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/countries"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/nodes"))).isTrue();
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
