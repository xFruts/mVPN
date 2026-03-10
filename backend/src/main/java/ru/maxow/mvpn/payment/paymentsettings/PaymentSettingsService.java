package ru.maxow.mvpn.payment.paymentsettings;


import ru.maxow.mvpn.model.CreateUpdatePaymentSettingsDto;
import ru.maxow.mvpn.model.PaymentSettingsResponseDto;

public interface PaymentSettingsService {
  PaymentSettingsResponseDto getPaymentSettings(String billingMonth);

  PaymentSettingsResponseDto createPaymentSettings(CreateUpdatePaymentSettingsDto dto);

  PaymentSettingsResponseDto updatePaymentSettings(Long id, CreateUpdatePaymentSettingsDto dto);


}
