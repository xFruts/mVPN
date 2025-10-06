package ru.maxow.mvpn.notification;

import java.time.LocalDate;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.adapter.telegram.TelegramSenderService;
import ru.maxow.mvpn.broadcast.Broadcast;
import ru.maxow.mvpn.broadcast.BroadcastRequestDto;
import ru.maxow.mvpn.broadcast.TargetAudience;
import ru.maxow.mvpn.payment.PaymentSettingsResponseDto;
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
  public static final String BROADCAST_TOPIC_NAME = "broadcast-requests";

  PaymentSettingsService paymentSettingsService;
  KafkaTemplate<String, BroadcastRequestDto> kafkaTemplate;

  /**
   * Sends payment reminders to all regular users on the payment date.
   * Scheduled to run daily at 9 AM.
   */
  @Scheduled(cron = "0 0 * * * ?")
  public void sendPaymentReminders() {
    PaymentSettingsResponseDto settings;
    try {
      settings = paymentSettingsService.getLatestPaymentSettings();
    } catch (NotFoundException e) {
      log.warn("Payment settings not found. Skipping payment reminders.");
      return;
    }

    LocalDate paymentDate = LocalDate.parse(settings.paymentDate());

    if (LocalDate.now().getDayOfMonth() == paymentDate.getDayOfMonth()) {
      BroadcastRequestDto request = getBroadcastPaymentMessage(paymentDate, settings);

      log.info("Sending payment reminder request to Kafka topic: {}", BROADCAST_TOPIC_NAME);
      kafkaTemplate.send(BROADCAST_TOPIC_NAME, request);
    }
  }

  @NotNull
  private static BroadcastRequestDto getBroadcastPaymentMessage(LocalDate paymentDate, PaymentSettingsResponseDto settings) {
    String messageText = String.format(
        """
           Здравствуйте! Напоминаем, что (%s) истекает срок оплаты услуги.
           Пожалуйста, произведите оплату в ближайшее время, чтобы избежать прерывания сервиса.
           К оплате: %.2f руб.
           Реквизиты для оплаты:
           Банк: %s
           Номер телефона: %d
           Спасибо за понимание!
        """, paymentDate, settings.price(), settings.bankName(), settings.phoneNumber()
    );

    return new BroadcastRequestDto(messageText, TargetAudience.REGULAR, List.of());
  }

}
