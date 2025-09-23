package ru.maxow.mvpn.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.adapter.telegram.TelegramSenderService;
import ru.maxow.mvpn.payment.PaymentSettings;
import ru.maxow.mvpn.payment.PaymentSettingsService;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private PaymentSettingsService paymentSettingsService;

    @Mock
    private TelegramSenderService telegramSenderService;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendPaymentReminders_ShouldSendMessages_WhenDateMatches() {
        // Given
        LocalDate today = LocalDate.now();
        PaymentSettings settings = new PaymentSettings();
        settings.setPaymentDate(today);
        settings.setPrice(100.0);
        settings.setBankName("Test Bank");
        settings.setPhoneNumber(1234567890L);

        User user1 = new User();
        user1.setUserTelegramId(1L);
        User user2 = new User();
        user2.setUserTelegramId(2L);
        List<User> users = List.of(user1, user2);

        when(paymentSettingsService.getPaymentSettings(1L)).thenReturn(settings);
        when(userService.getRegularUsers()).thenReturn(users);

        // When
        notificationService.sendPaymentReminders();

        // Then
        verify(userService, times(1)).getRegularUsers();
        verify(telegramSenderService, times(2)).sendMessage(anyString(), anyString());
    }

    @Test
    void sendPaymentReminders_ShouldNotSendMessages_WhenDateDoesNotMatch() {
        // Given
        LocalDate differentDate = LocalDate.now().plusDays(1);
        PaymentSettings settings = new PaymentSettings();
        settings.setPaymentDate(differentDate);

        when(paymentSettingsService.getPaymentSettings(1L)).thenReturn(settings);

        // When
        notificationService.sendPaymentReminders();

        // Then
        verify(userService, never()).getRegularUsers();
        verify(telegramSenderService, never()).sendMessage(anyString(), anyString());
    }

    @Test
    void sendPaymentReminders_ShouldDoNothing_WhenSettingsNotFound() {
        // Given
        when(paymentSettingsService.getPaymentSettings(1L)).thenThrow(new NotFoundException("PaymentSettings", 1L));

        // When
        notificationService.sendPaymentReminders();

        // Then
        verify(userService, never()).getRegularUsers();
        verify(telegramSenderService, never()).sendMessage(anyString(), anyString());
    }
}

