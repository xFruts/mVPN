package ru.maxow.mvpn.payment;

import java.util.Optional;

public interface PaymentSettingsService {
  PaymentSettings getPaymentSettings(Long id);
  PaymentSettings createPaymentSettings(PaymentSettingsRequestDto paymentSettingsRequestDto);
  PaymentSettings updatePaymentSettings(Long id, PaymentSettingsRequestDto paymentSettingsRequestDto);
  void deletePaymentSettings(Long id);
}
