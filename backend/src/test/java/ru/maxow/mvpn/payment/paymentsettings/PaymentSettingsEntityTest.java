package ru.maxow.mvpn.payment.paymentsettings;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentSettings entity - unit tests")
class PaymentSettingsEntityTest {

  @Test
  @DisplayName("Given createdAt is null When onCreate Then set current timestamp")
  void givenNullCreatedAtWhenOnCreateThenSetTimestamp() {
    PaymentSettings settings = new PaymentSettings();

    settings.onCreate();

    assertThat(settings.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Given createdAt already set When onCreate Then keep original timestamp")
  void givenExistingCreatedAtWhenOnCreateThenKeepOriginalValue() {
    PaymentSettings settings = new PaymentSettings();
    Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
    settings.setCreatedAt(fixed);

    settings.onCreate();

    assertThat(settings.getCreatedAt()).isEqualTo(fixed);
  }
}

