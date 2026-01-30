package ru.maxow.mvpn.payment.paymentsettings;

import ru.maxow.mvpn.payment.paymentsettings.dto.CreateUpdatePaymentSettingsDto;
import ru.maxow.mvpn.payment.paymentsettings.dto.PaymentSettingsResponseDto;

public interface PaymentSettingsService {
  PaymentSettingsResponseDto getPaymentSettings(String billingMonth);

  PaymentSettingsResponseDto createPaymentSettings(CreateUpdatePaymentSettingsDto dto);

  PaymentSettingsResponseDto updatePaymentSettings(Long id, CreateUpdatePaymentSettingsDto dto);


}
