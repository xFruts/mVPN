package ru.maxow.mvpn.telegram.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.telegram.service.TelegramKeyboardFactory;
import ru.maxow.mvpn.telegram.service.TelegramMessageFormatter;
import ru.maxow.mvpn.telegram.service.TelegramUserService;
import ru.maxow.mvpn.user.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionCommandHandler - unit tests")
class SubscriptionCommandHandlerTest {

  @Mock
  private TelegramUserService userService;

  private final TelegramMessageFormatter messageFormatter = new TelegramMessageFormatter();
  private final TelegramKeyboardFactory keyboardFactory = new TelegramKeyboardFactory();

  private SubscriptionCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new SubscriptionCommandHandler(userService, messageFormatter, keyboardFactory);
  }

  @Test
  @DisplayName("Given unauthenticated user When handle Then return auth prompt")
  void givenUnauthenticatedUserWhenHandleThenReturnAuthPrompt() {
    Update update = updateWithText(100L, "/status");
    when(userService.findAuthenticatedUser(eq(100L), any())).thenReturn(Optional.empty());

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("100");
      assertThat(message.getText()).contains("/auth");
    });
  }

  @Test
  @DisplayName("Given authenticated user with active subscription When handle Then return status")
  void givenUserWithSubscriptionWhenHandleThenReturnFormattedStatus() {
    Update update = updateWithText(100L, "/status");
    User user = new User();
    user.setId(5L);

    Tariff tariff = new Tariff();
    tariff.setName("Standard-30");
    tariff.setTrafficLimitGb(30);

    Subscription subscription = new Subscription();
    subscription.setId(42L);
    subscription.setTariff(tariff);
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setStartDate(OffsetDateTime.now(ZoneOffset.UTC).minusDays(5));
    subscription.setEndDate(OffsetDateTime.now(ZoneOffset.UTC).plusDays(25));

    SubscriptionTrafficState trafficState = new SubscriptionTrafficState();
    trafficState.setUsedBytes(5L * 1024 * 1024 * 1024);

    when(userService.findAuthenticatedUser(eq(100L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.of(subscription));
    when(userService.getTrafficState(42L)).thenReturn(Optional.of(trafficState));

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("100");
      assertThat(message.getText()).contains("Standard-30");
      assertThat(message.getText()).contains("Активна");
      assertThat(message.getText()).contains("5.00 ГБ");
    });
  }

  @Test
  @DisplayName("Given authenticated user without subscription When handle Then return empty")
  void givenUserWithoutSubscriptionWhenHandleThenReturnEmptyMessage() {
    Update update = updateWithText(100L, "/status");
    User user = new User();
    user.setId(5L);

    when(userService.findAuthenticatedUser(eq(100L), any())).thenReturn(Optional.of(user));
    when(userService.findLatestSubscription(user)).thenReturn(Optional.empty());

    List<SendMessage> result = handler.handle(update);

    assertThat(result).singleElement().satisfies(message -> {
      assertThat(message.getChatId()).isEqualTo("100");
      assertThat(message.getText()).contains("нет активной подписки");
    });
  }

  @Test
  @DisplayName("Given supported texts When supports Then return true")
  void givenSupportedTextsWhenSupportsThenTrue() {
    assertThat(handler.supports(updateWithText(1L, "/status"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/subscription"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/sub"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, "/my"))).isTrue();
    assertThat(handler.supports(updateWithText(1L, TelegramKeyboardFactory.BTN_SUBSCRIPTION)))
        .isTrue();
    assertThat(handler.supports(updateWithText(1L, "/other"))).isFalse();
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
}
