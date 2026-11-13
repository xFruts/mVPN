package ru.maxow.mvpn.telegram.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.user.User;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ConnectionCommandHandler - unit tests")
class ConnectionCommandHandlerTest {

  @Mock
  private TelegramUserService userService;

  private final TelegramMessageFormatter messageFormatter = new TelegramMessageFormatter();
  private final TelegramKeyboardFactory keyboardFactory = new TelegramKeyboardFactory();

  private ConnectionCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new ConnectionCommandHandler(userService, messageFormatter, keyboardFactory);
  }

  @Test
  @DisplayName("Given unauthenticated user When handle Then return prompt to /auth")
  void givenUnauthenticatedUserWhenHandleThenReturnAuthPrompt() {
    Update update = updateWithText(100L, "/connect");
    when(userService.findAuthenticatedUser(100L)).thenReturn(Optional.empty());

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("100");
      assertThat(message.getText()).contains("Вы ещё не авторизованы");
    });
  }

  @Test
  @DisplayName("Given user with active subscription When handle Then return connection link")
  void givenUserWithSubscriptionWhenHandleThenReturnConnectionGuide() {
    Update update = updateWithText(200L, TelegramKeyboardFactory.BTN_CONNECTION);
    User user = new User();
    user.setId(3L);

    Subscription sub = new Subscription();
    sub.setStatus(SubscriptionStatus.ACTIVE);
    sub.setStartDate(OffsetDateTime.now(ZoneOffset.UTC));
    sub.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

    when(userService.findAuthenticatedUser(200L)).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.of(sub));
    when(userService.buildSubscriptionUrl(user))
        .thenReturn("https://api.example.com/v1/config/uuid-code");

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("200");
      assertThat(message.getText()).contains("Подключение к VPN");
      assertThat(message.getText()).contains("https://api.example.com/v1/config/uuid-code");
      assertThat(message.getText()).contains("v2rayNG");
    });
  }

  @Test
  @DisplayName("Given user without active subscription When handle Then return warning")
  void givenUserWithoutActiveSubscriptionWhenHandleThenReturnWarning() {
    Update update = updateWithText(300L, "/link");
    User user = new User();

    when(userService.findAuthenticatedUser(300L)).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.empty());
    when(userService.buildSubscriptionUrl(user)).thenReturn("");

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message ->
        assertThat(message.getText()).contains("нет активной подписки"));
  }

  @Test
  @DisplayName("Given supported texts When supports Then return true")
  void givenSupportedTextsWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "/connect"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/config"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/link"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/getkey"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, TelegramKeyboardFactory.BTN_CONNECTION)))
        .isTrue();
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
