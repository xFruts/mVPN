package ru.maxow.mvpn.telegram.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
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
    when(userService.findAuthenticatedUser(eq(100L), any())).thenReturn(Optional.empty());

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

    when(userService.findAuthenticatedUser(eq(200L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.of(sub));
    when(userService.buildSubscriptionUrl(user))
        .thenReturn("https://api.example.com/v1/config/uuid-code");

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("200");
      assertThat(message.getText()).contains("Подключение к mVPN");
      assertThat(message.getText()).contains("https://api.example.com/v1/config/uuid-code");
      assertThat(message.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);
    });
  }

  @Test
  @DisplayName("Given user without active subscription When handle Then return warning")
  void givenUserWithoutActiveSubscriptionWhenHandleThenReturnWarning() {
    Update update = updateWithText(300L, "/link");
    User user = new User();

    when(userService.findAuthenticatedUser(eq(300L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.empty());
    when(userService.buildSubscriptionUrl(user)).thenReturn("");

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message ->
        assertThat(message.getText()).contains("нет активной подписки"));
  }

  @Test
  @DisplayName("Given /happ command When handle Then return Happ guide")
  void givenHappCommandWhenHandleThenReturnHappGuide() {
    Update update = updateWithText(200L, "/happ");
    User user = new User();
    Subscription sub = new Subscription();
    sub.setStatus(SubscriptionStatus.ACTIVE);
    sub.setStartDate(OffsetDateTime.now(ZoneOffset.UTC));
    sub.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

    when(userService.findAuthenticatedUser(eq(200L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.of(sub));
    when(userService.buildSubscriptionUrl(user))
        .thenReturn("https://api.example.com/v1/config/uuid-code");

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getText()).contains("Happ");
      assertThat(message.getText()).contains("Добавить подписку");
      assertThat(message.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);
    });
  }

  @Test
  @DisplayName("Given /v2raytun command When handle Then return v2rayTun guide")
  void givenV2rayTunCommandWhenHandleThenReturnV2rayTunGuide() {
    Update update = updateWithText(200L, "/v2raytun");
    User user = new User();
    Subscription sub = new Subscription();
    sub.setStatus(SubscriptionStatus.ACTIVE);
    sub.setStartDate(OffsetDateTime.now(ZoneOffset.UTC));
    sub.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

    when(userService.findAuthenticatedUser(eq(200L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.of(sub));
    when(userService.buildSubscriptionUrl(user))
        .thenReturn("https://api.example.com/v1/config/uuid-code");

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getText()).contains("v2rayTun");
      assertThat(message.getText()).contains("Обновить подписку");
      assertThat(message.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);
    });
  }

  @Test
  @DisplayName("Given /key command When handle Then return direct VLESS key")
  void givenKeyCommandWhenHandleThenReturnVlessKey() {
    Update update = updateWithText(200L, "/key");
    User user = new User();
    Subscription sub = new Subscription();
    sub.setStatus(SubscriptionStatus.ACTIVE);
    sub.setStartDate(OffsetDateTime.now(ZoneOffset.UTC));
    sub.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

    when(userService.findAuthenticatedUser(eq(200L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.of(sub));
    when(userService.buildSubscriptionUrl(user))
        .thenReturn("https://api.example.com/v1/config/uuid-code");
    when(userService.getDirectVlessKey(user))
        .thenReturn(Optional.of("vless://user-uuid@host:443?type=tcp"));

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getText()).contains("vless://user-uuid@host:443?type=tcp");
      assertThat(message.getText()).contains("Импорт из буфера обмена");
    });
  }

  @Test
  @DisplayName("Given callback query When handle Then return appropriate guide")
  void givenCallbackQueryWhenHandleThenReturnGuide() {
    Update update = updateWithCallback(200L, TelegramKeyboardFactory.CB_GUIDE_HAPP);
    User user = new User();
    Subscription sub = new Subscription();
    sub.setStatus(SubscriptionStatus.ACTIVE);
    sub.setStartDate(OffsetDateTime.now(ZoneOffset.UTC));
    sub.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));

    when(userService.findAuthenticatedUser(eq(200L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.of(sub));
    when(userService.buildSubscriptionUrl(user))
        .thenReturn("https://api.example.com/v1/config/uuid-code");

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getText()).contains("Happ");
      assertThat(message.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);
    });
  }

  @Test
  @DisplayName("Given supported texts and callbacks When supports Then return true")
  void givenSupportedTextsWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "/connect"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/config"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/link"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/getkey"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/key"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/vless"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/happ"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/v2raytun"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, TelegramKeyboardFactory.BTN_CONNECTION)))
        .isTrue();
    assertThat(handler.supports(updateWithCallback(1L, TelegramKeyboardFactory.CB_GUIDE_HAPP)))
        .isTrue();
    assertThat(handler.supports(updateWithCallback(1L, TelegramKeyboardFactory.CB_GUIDE_V2RAYTUN)))
        .isTrue();
    assertThat(handler.supports(updateWithCallback(1L, TelegramKeyboardFactory.CB_GET_VLESS_KEY)))
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

  private Update updateWithCallback(Long chatId, String callbackData) {
    Update update = mock(Update.class);
    CallbackQuery callbackQuery = mock(CallbackQuery.class);
    Message message = mock(Message.class);

    when(update.hasCallbackQuery()).thenReturn(true);
    when(update.getCallbackQuery()).thenReturn(callbackQuery);
    when(callbackQuery.getData()).thenReturn(callbackData);
    when(callbackQuery.getMessage()).thenReturn(message);
    when(message.getChatId()).thenReturn(chatId);

    return update;
  }
}
