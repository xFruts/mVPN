package ru.maxow.mvpn.payment;

/**
 * Service interface for managing payment settings.
 */
public interface PaymentSettingsService {
  /** Retrieves payment settings by ID. */
  PaymentSettings getPaymentSettings(Long id);

  /** Creates new payment settings. */
  PaymentSettings createPaymentSettings(PaymentSettingsRequestDto paymentSettingsRequestDto);

  /** Updates existing payment settings. */
  PaymentSettings updatePaymentSettings(
      Long id, PaymentSettingsRequestDto paymentSettingsRequestDto);

  /** Deletes payment settings by ID. */
  void deletePaymentSettings(Long id);
}
