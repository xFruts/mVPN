package ru.maxow.mvpn.notification;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.adapter.telegram.TelegramSenderService;
import ru.maxow.mvpn.payment.PaymentSettings;
import ru.maxow.mvpn.payment.PaymentSettingsService;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;
import ru.maxow.mvpn.util.exception.NotFoundException;


/**
 * Service for sending notifications to users.
 * Currently, it sends payment reminders on a scheduled basis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {
  UserService userService;
  PaymentSettingsService paymentSettingsService;
  TelegramSenderService telegramSenderService;

  /**
   * Sends payment reminders to all regular users on the payment date.
   * Scheduled to run daily at 9 AM.
   */
  @Scheduled(cron = "0 0 9 * * ?")
  public void sendPaymentReminders() {
    PaymentSettings settings;
    try {
      settings = paymentSettingsService.getPaymentSettings(1L);
    } catch (NotFoundException e) {
      log.warn("Payment settings not found. Skipping payment reminders.");
      return;
    }

    LocalDate paymentDate = settings.getPaymentDate();

    if (LocalDate.now().getDayOfMonth() == paymentDate.getDayOfMonth()) {
      List<User> users = userService.getRegularUsers();
      String messageText = String.format(
          """
             Здравствуйте! Напоминаем, что (%s) истекает срок оплаты услуги.
             Пожалуйста, произведите оплату в ближайшее время, чтобы избежать прерывания сервиса.
             К оплате: %.2f руб.
             Реквизиты для оплаты:
             Банк: %s
             Номер телефона: %d
             Спасибо за понимание!
          """, paymentDate, settings.getPrice(), settings.getBankName(), settings.getPhoneNumber()
      );

      for (User user : users) {
        telegramSenderService.sendMessage(user.getUserTelegramId().toString(), messageText);
      }
      log.info("Sent payment reminders to {} users.", users.size());
    }
  }

}
