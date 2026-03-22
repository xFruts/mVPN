package ru.maxow.mvpn.payment.paymentverification;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentVerification entity - unit tests")
class PaymentVerificationEntityTest {

  @Test
  @DisplayName("Given createdAt is null When onCreate Then set current timestamp")
  void givenNullCreatedAtWhenOnCreateThenSetTimestamp() {
    PaymentVerification verification = new PaymentVerification();

    verification.onCreate();

    assertThat(verification.getCreatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Given createdAt already set When onCreate Then keep original timestamp")
  void givenExistingCreatedAtWhenOnCreateThenKeepOriginalValue() {
    PaymentVerification verification = new PaymentVerification();
    Instant fixed = Instant.parse("2026-01-01T00:00:00Z");
    verification.setCreatedAt(fixed);

    verification.onCreate();

    assertThat(verification.getCreatedAt()).isEqualTo(fixed);
  }
}

