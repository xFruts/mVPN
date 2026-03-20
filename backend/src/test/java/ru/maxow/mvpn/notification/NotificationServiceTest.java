package ru.maxow.mvpn.notification;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.payment.paymentsettings.PaymentSettingsService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService - Unit тесты (бизнес-логика)")
class NotificationServiceTest {

  @Mock
  private PaymentSettingsService paymentSettingsService;

  @BeforeEach
  void setUp() {}

  @Nested
  @DisplayName("Текущий контракт сервиса")
  class CurrentContractTests {

    @Test
    @DisplayName("BROADCAST_TOPIC_NAME должен быть равен broadcast-requests")
    void shouldExposeExpectedBroadcastTopicName() {
      assertThat(NotificationService.BROADCAST_TOPIC_NAME)
          .isEqualTo("broadcast-requests");
    }

    @Test
    @DisplayName("Конструктор должен сохранять ссылку на PaymentSettingsService")
    void shouldKeepInjectedPaymentSettingsServiceReference() throws Exception {
      NotificationService service = new NotificationService(paymentSettingsService);

      Field dependencyField = NotificationService.class.getDeclaredField("paymentSettingsService");
      dependencyField.setAccessible(true);

      Object injectedDependency = dependencyField.get(service);
      assertThat(injectedDependency).isSameAs(paymentSettingsService);
    }
  }
}

