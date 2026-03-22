package ru.maxow.mvpn.broadcast;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.BroadcastRequestDto;
import ru.maxow.mvpn.model.TargetAudience;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.telegram.adapter.TelegramSenderService;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BroadcastListener - Unit тесты")
class BroadcastListenerTest {

  @Mock
  private UserService userService;

  @Mock
  private TelegramSenderService senderService;

  @InjectMocks
  private BroadcastListener listener;

  @Test
  @DisplayName("Должен отправить сообщение всем пользователям с telegramId")
  void shouldSendMessageToAllUsersWithTelegramId() {
    BroadcastRequestDto dto = new BroadcastRequestDto();
    dto.setMessage("Maintenance tonight");
    dto.setTargetAudience(TargetAudience.ALL);

    User withTelegram = new User();
    withTelegram.setId(1L);
    withTelegram.setUserTelegramId(123456L);

    User withoutTelegram = new User();
    withoutTelegram.setId(2L);
    withoutTelegram.setUserTelegramId(null);

    when(userService.findAll()).thenReturn(List.of(withTelegram, withoutTelegram));

    listener.handleBroadcast(dto);

    verify(userService).findAll();
    verify(senderService).sendMessage("123456", "Maintenance tonight");
  }

  @Test
  @DisplayName("Не должен отправлять сообщение, если request пустой")
  void shouldNotSendWhenRequestIsInvalid() {
    BroadcastRequestDto dto = new BroadcastRequestDto();
    dto.setTargetAudience(TargetAudience.ALL);

    listener.handleBroadcast(dto);

    verify(userService, never()).findAll();
    verify(senderService, never()).sendMessage(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("Given REGULAR audience When handleBroadcast Then query regular users")
  void givenRegularAudienceWhenHandleBroadcastThenQueryRegularUsers() {
    BroadcastRequestDto dto = new BroadcastRequestDto();
    dto.setMessage("Hello regulars");
    dto.setTargetAudience(TargetAudience.REGULAR);

    User user = new User();
    user.setId(10L);
    user.setUserTelegramId(1010L);
    when(userService.getUsersByRole(UserRole.REGULAR)).thenReturn(List.of(user));

    listener.handleBroadcast(dto);

    verify(userService).getUsersByRole(UserRole.REGULAR);
    verify(senderService).sendMessage("1010", "Hello regulars");
  }

  @Test
  @DisplayName("Given VIP audience When handleBroadcast Then query special users")
  void givenVipAudienceWhenHandleBroadcastThenQuerySpecialUsers() {
    BroadcastRequestDto dto = new BroadcastRequestDto();
    dto.setMessage("Hello vip");
    dto.setTargetAudience(TargetAudience.VIP);

    User user = new User();
    user.setId(11L);
    user.setUserTelegramId(1111L);
    when(userService.getUsersByRole(UserRole.SPECIAL)).thenReturn(List.of(user));

    listener.handleBroadcast(dto);

    verify(userService).getUsersByRole(UserRole.SPECIAL);
    verify(senderService).sendMessage("1111", "Hello vip");
  }

  @Test
  @DisplayName("Given CUSTOM_LIST with ids When handleBroadcast Then query only selected telegram ids")
  void givenCustomListWhenHandleBroadcastThenQueryByTelegramIds() {
    BroadcastRequestDto dto = new BroadcastRequestDto();
    dto.setMessage("Custom");
    dto.setTargetAudience(TargetAudience.CUSTOM_LIST);
    dto.setCustomUserIds(List.of(555L, 777L));

    User user = new User();
    user.setId(12L);
    user.setUserTelegramId(555L);
    when(userService.getUsersByTelegramIds(List.of(555L, 777L))).thenReturn(List.of(user));

    listener.handleBroadcast(dto);

    verify(userService).getUsersByTelegramIds(List.of(555L, 777L));
    verify(senderService).sendMessage("555", "Custom");
  }

  @Test
  @DisplayName("Given CUSTOM_LIST without ids When handleBroadcast Then do not query users and do not send")
  void givenEmptyCustomListWhenHandleBroadcastThenNoSend() {
    BroadcastRequestDto dto = new BroadcastRequestDto();
    dto.setMessage("Custom");
    dto.setTargetAudience(TargetAudience.CUSTOM_LIST);
    dto.setCustomUserIds(List.of());

    listener.handleBroadcast(dto);

    verify(userService, never()).getUsersByTelegramIds(List.of());
    verify(senderService, never()).sendMessage(anyString(), anyString());
  }

  @Test
  @DisplayName("Given send failure for one user When handleBroadcast Then continue for remaining users")
  void givenSingleSendFailureWhenHandleBroadcastThenContinueSending() {
    BroadcastRequestDto dto = new BroadcastRequestDto();
    dto.setMessage("Maintenance");
    dto.setTargetAudience(TargetAudience.ALL);

    User first = new User();
    first.setId(1L);
    first.setUserTelegramId(111L);

    User second = new User();
    second.setId(2L);
    second.setUserTelegramId(222L);

    when(userService.findAll()).thenReturn(List.of(first, second));
    doThrow(new RuntimeException("telegram down"))
        .when(senderService).sendMessage("111", "Maintenance");

    listener.handleBroadcast(dto);

    verify(senderService, times(1)).sendMessage("111", "Maintenance");
    verify(senderService, times(1)).sendMessage("222", "Maintenance");
  }
}

