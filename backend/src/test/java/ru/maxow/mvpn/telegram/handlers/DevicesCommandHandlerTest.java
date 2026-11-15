package ru.maxow.mvpn.telegram.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.telegram.model.UserDeviceRecord;
import ru.maxow.mvpn.telegram.model.UserDevicesSummary;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.user.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("DevicesCommandHandler - unit tests")
class DevicesCommandHandlerTest {

  @Mock
  private TelegramUserService userService;

  private final TelegramMessageFormatter messageFormatter = new TelegramMessageFormatter();
  private final TelegramKeyboardFactory keyboardFactory = new TelegramKeyboardFactory();

  private DevicesCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new DevicesCommandHandler(userService, messageFormatter, keyboardFactory);
  }

  @Test
  @DisplayName("Given unauthenticated user When handle Then return unauthorized prompt")
  void givenUnauthenticatedUserWhenHandleThenReturnUnauthorized() {
    Update update = updateWithText(100L, "/devices");
    when(userService.findAuthenticatedUser(eq(100L), any())).thenReturn(Optional.empty());

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(SendMessage.class);
    SendMessage message = (SendMessage) result.get(0);
    assertThat(message.getChatId()).isEqualTo("100");
    assertThat(message.getText()).contains("/auth");
  }

  @Test
  @DisplayName("Given user without active subscription When handle Then return no subscription")
  void givenUserWithoutActiveSubscriptionWhenHandleThenReturnNoSubscription() {
    Update update = updateWithText(100L, "/devices");
    User user = new User();
    user.setId(10L);

    when(userService.findAuthenticatedUser(eq(100L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.empty());

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(SendMessage.class);
    SendMessage message = (SendMessage) result.get(0);
    assertThat(message.getChatId()).isEqualTo("100");
    assertThat(message.getText()).contains("нет активной подписки");
  }

  @Test
  @DisplayName("Given active subscription and text /devices When handle Then return device card")
  void givenActiveSubscriptionWhenHandleTextThenReturnDevicesMessage() {
    Update update = updateWithText(100L, "/devices");
    User user = new User();
    user.setId(10L);

    Subscription sub = new Subscription();
    sub.setStatus(SubscriptionStatus.ACTIVE);
    Tariff tariff = new Tariff();
    tariff.setMaxDevices(2);
    sub.setTariff(tariff);

    UserDevicesSummary summary = new UserDevicesSummary(
        1,
        2,
        1,
        List.of(new UserDeviceRecord("198.51.100.1", List.of("🇫🇮 Finland"), 1700000000L))
    );

    when(userService.findAuthenticatedUser(eq(100L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.of(sub));
    when(userService.getConnectedDevices(user)).thenReturn(summary);

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(SendMessage.class);
    SendMessage message = (SendMessage) result.get(0);
    assertThat(message.getChatId()).isEqualTo("100");
    assertThat(message.getText()).contains("198.51.100.1");
    assertThat(message.getText()).contains("Finland");
    assertThat(message.getText()).contains("Активных устройств: 1");
    assertThat(message.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);
  }

  @Test
  @DisplayName("Given callback query refresh When handle Then edit message in place")
  void givenCallbackQueryWhenHandleThenEditMessageInPlace() {
    Update update = updateWithCallback(100L, 777, TelegramKeyboardFactory.CB_REFRESH_DEVICES);
    User user = new User();
    user.setId(10L);

    Subscription sub = new Subscription();
    sub.setStatus(SubscriptionStatus.ACTIVE);
    Tariff tariff = new Tariff();
    tariff.setMaxDevices(3);
    sub.setTariff(tariff);

    UserDevicesSummary summary = new UserDevicesSummary(
        0,
        3,
        3,
        List.of()
    );

    when(userService.findAuthenticatedUser(eq(100L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.of(sub));
    when(userService.getConnectedDevices(user)).thenReturn(summary);

    List<BotApiMethod<?>> result = handler.handle(update);

    assertThat(result).singleElement().isInstanceOf(EditMessageText.class);
    EditMessageText edit = (EditMessageText) result.get(0);
    assertThat(edit.getChatId()).isEqualTo("100");
    assertThat(edit.getMessageId()).isEqualTo(777);
    assertThat(edit.getText()).contains("Устройства пока не подключены");
    assertThat(edit.getReplyMarkup()).isNotNull();
  }

  @Test
  @DisplayName("Given commands and callbacks When supports Then verify detection")
  void givenSupportedInputsWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "/devices"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/device"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/ips"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/ip"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "📱 Мои устройства"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "📱 My Devices"))).isTrue();
    assertThat(handler.supports(updateWithCallback(
        1L, 1, TelegramKeyboardFactory.CB_DEVICES))).isTrue();
    assertThat(handler.supports(updateWithCallback(
        1L, 1, TelegramKeyboardFactory.CB_REFRESH_DEVICES))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/clear_devices"))).isFalse();
    assertThat(handler.supports(updateWithText(1L, "/status"))).isFalse();
  }

  private Update updateWithText(Long chatId, String text) {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    lenient().when(update.hasMessage()).thenReturn(true);
    lenient().when(update.getMessage()).thenReturn(message);
    lenient().when(message.hasText()).thenReturn(true);
    lenient().when(message.getText()).thenReturn(text);
    lenient().when(message.getChatId()).thenReturn(chatId);

    return update;
  }

  private Update updateWithCallback(Long chatId, Integer messageId, String data) {
    Update update = mock(Update.class);
    CallbackQuery callback = mock(CallbackQuery.class);
    Message message = mock(Message.class);

    lenient().when(update.hasCallbackQuery()).thenReturn(true);
    lenient().when(update.getCallbackQuery()).thenReturn(callback);
    lenient().when(callback.getData()).thenReturn(data);
    lenient().when(callback.getMessage()).thenReturn(message);
    lenient().when(message.getChatId()).thenReturn(chatId);
    lenient().when(message.getMessageId()).thenReturn(messageId);

    return update;
  }
}
