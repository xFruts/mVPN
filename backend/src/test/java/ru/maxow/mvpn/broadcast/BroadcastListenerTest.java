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
import ru.maxow.mvpn.telegram.adapter.TelegramSenderService;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;

import static org.mockito.Mockito.never;
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
}

