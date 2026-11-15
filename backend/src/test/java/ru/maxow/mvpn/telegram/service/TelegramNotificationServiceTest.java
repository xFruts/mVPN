package ru.maxow.mvpn.telegram.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.OffsetDateTime;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import ru.maxow.mvpn.device.UserDevice;
import ru.maxow.mvpn.telegram.adapter.TelegramSenderService;
import ru.maxow.mvpn.user.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramNotificationService - модульные тесты")
class TelegramNotificationServiceTest {

  @Mock
  private TelegramSenderService senderService;

  private final TelegramKeyboardFactory keyboardFactory = new TelegramKeyboardFactory();
  private TelegramNotificationService notificationService;

  @BeforeEach
  void setUp() {
    ResourceBundleMessageSource bundleSource = new ResourceBundleMessageSource();
    bundleSource.setBasenames("messages");
    bundleSource.setDefaultEncoding("UTF-8");
    bundleSource.setUseCodeAsDefaultMessage(true);
    bundleSource.setFallbackToSystemLocale(false);
    bundleSource.setDefaultLocale(Locale.forLanguageTag("ru"));

    notificationService = new TelegramNotificationService(
        senderService,
        keyboardFactory,
        bundleSource
    );
  }

  @Test
  @DisplayName("Уведомление отправляется при наличии telegramId у пользователя")
  void givenUserWithTelegramIdWhenNotifyThenSendMessage() {
    User user = new User();
    user.setId(10L);
    user.setUserTelegramId(999888777L);

    UserDevice device = new UserDevice();
    device.setDeviceModel("MaxowPC_x86_64");
    device.setOsName("Windows 11");
    device.setClientApp("Happ 4.5.4");
    device.setLastIsp("Ростелеком");
    device.setLastCity("Москва");
    device.setFirstSeenAt(OffsetDateTime.now());

    notificationService.notifyNewDeviceConnected(user, device);

    verify(senderService).sendMessage(
        eq("999888777"),
        contains("Подключено новое устройство"),
        any()
    );
  }

  @Test
  @DisplayName("Уведомление не отправляется при отсутствии telegramId у пользователя")
  void givenUserWithoutTelegramIdWhenNotifyThenDoNothing() {
    User user = new User();
    user.setId(10L);
    user.setUserTelegramId(null);

    UserDevice device = new UserDevice();
    notificationService.notifyNewDeviceConnected(user, device);

    verifyNoInteractions(senderService);
  }
}
