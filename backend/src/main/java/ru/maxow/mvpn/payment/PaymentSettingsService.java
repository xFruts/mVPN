package ru.maxow.mvpn.payment;

/**
 * Service interface for managing payment settings.
 */
public interface PaymentSettingsService {
  /** Retrieves the latest payment settings as a DTO. */
  PaymentSettingsResponseDto getLatestPaymentSettings();

  /** Creates or updates payment settings. */
  PaymentSettingsResponseDto createOrUpdateLatestPaymentSettings(
      PaymentSettingsRequestDto paymentSettingsRequestDto);

  /** Deletes payment settings by ID. */
  void deleteLatestPaymentSettings();
}
