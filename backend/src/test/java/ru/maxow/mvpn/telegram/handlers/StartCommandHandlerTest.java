package ru.maxow.mvpn.telegram.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.user.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StartCommandHandler - unit tests")
class StartCommandHandlerTest {

  @Mock
  private TelegramUserService userService;

  private final TelegramMessageFormatter messageFormatter = new TelegramMessageFormatter();
  private final TelegramKeyboardFactory keyboardFactory = new TelegramKeyboardFactory();

  private StartCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new StartCommandHandler(userService, messageFormatter, keyboardFactory);
  }

  @Test
  @DisplayName("Given /start When unauthenticated Then return unauthenticated welcome message")
  void givenStartWhenUnauthenticatedThenWelcomeWithAuthPrompt() {
    Update update = updateForHandle(12L, "/start", "Ivan", null);
    when(userService.findAuthenticatedUser(eq(12L), any())).thenReturn(Optional.empty());

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(SendMessage.class);
    SendMessage message = (SendMessage) result.get(0);
    assertThat(message.getChatId()).isEqualTo("12");
    assertThat(message.getText()).contains("Привет, Ivan!");
    assertThat(message.getText()).contains("/auth");
    assertThat(message.getReplyMarkup()).isNotNull();
  }

  @Test
  @DisplayName("Given /start When authenticated Then personalized welcome and main keyboard")
  void givenStartWhenAuthenticatedThenWelcomeWithMainMenu() {
    Update update = updateForHandle(55L, "/start", "Anna", null);
    User user = new User();
    user.setFullName("Anna Test");
    when(userService.findAuthenticatedUser(eq(55L), any())).thenReturn(Optional.of(user));

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(SendMessage.class);
    SendMessage message = (SendMessage) result.get(0);
    assertThat(message.getText()).contains("С возвращением, Anna!");
    assertThat(message.getReplyMarkup()).isNotNull();
  }

  @Test
  @DisplayName("Given /start with username When auto-authenticated Then personalized welcome")
  void givenStartWithUsernameWhenAutoAuthenticatedThenPersonalizedWelcome() {
    Update update = updateForHandle(88L, "/start", "Max", "xFruts");
    User user = new User();
    user.setFullName("Max Owner");
    user.setUserTelegramId(88L);
    when(userService.findAuthenticatedUser(88L, "xFruts")).thenReturn(Optional.of(user));

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(SendMessage.class);
    SendMessage message = (SendMessage) result.get(0);
    assertThat(message.getText()).contains("С возвращением, Max!");
    assertThat(message.getReplyMarkup()).isNotNull();
  }

  @Test
  @DisplayName("Given /start with valid deep-link token When handle Then authenticate user")
  void givenStartWithDeepLinkTokenWhenHandleThenAuthenticateUser() {
    UUID token = UUID.randomUUID();
    Update update = updateForHandle(77L, "/start " + token, "Oleg", null);
    when(userService.authenticateUser(token, 77L)).thenReturn(true);

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(SendMessage.class);
    SendMessage message = (SendMessage) result.get(0);
    assertThat(message.getText()).contains("успешно");
    assertThat(message.getText()).contains("С возвращением, Oleg!");
  }

  @Test
  @DisplayName("Given /start text When supports Then return true")
  void givenStartTextWhenSupportsThenTrue() {
    assertThat(handler.supports(updateForSupports("/start"))).isTrue();
  }

  @Test
  @DisplayName("Given unsupported update When supports Then return false")
  void givenUnsupportedUpdateWhenSupportsThenFalse() {
    Update noText = mock(Update.class);
    Message noTextMessage = mock(Message.class);
    when(noText.hasMessage()).thenReturn(true);
    when(noText.getMessage()).thenReturn(noTextMessage);
    when(noTextMessage.hasText()).thenReturn(false);

    assertThat(handler.supports(noText)).isFalse();

    Update nonStart = updateForSupports("/help");
    assertThat(handler.supports(nonStart)).isFalse();
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

  private Update updateForHandle(Long chatId, String text, String firstName, String username) {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    org.telegram.telegrambots.meta.api.objects.User tgUser =
        mock(org.telegram.telegrambots.meta.api.objects.User.class);
    Chat chat = mock(Chat.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasText()).thenReturn(true);
    when(message.getText()).thenReturn(text);
    when(message.getChatId()).thenReturn(chatId);
    when(message.getChat()).thenReturn(chat);
    when(chat.getId()).thenReturn(chatId);
    when(chat.getFirstName()).thenReturn(firstName);

    when(message.getFrom()).thenReturn(tgUser);
    when(tgUser.getFirstName()).thenReturn(firstName);
    when(tgUser.getUserName()).thenReturn(username);

    return update;
  }
}
