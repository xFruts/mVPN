package ru.maxow.mvpn.payment.paymentsettings;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.PaymentSettingsApi;
import ru.maxow.mvpn.model.CreateUpdatePaymentSettingsDto;
import ru.maxow.mvpn.model.PaymentSettingsResponseDto;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentSettingsController implements PaymentSettingsApi {

  PaymentSettingsService paymentSettingsService;

  @Override
  public PaymentSettingsResponseDto createPaymentSettings(
      CreateUpdatePaymentSettingsDto createUpdatePaymentSettingsDto) {
    return paymentSettingsService.createPaymentSettings(createUpdatePaymentSettingsDto);
  }

  @Override
  public PaymentSettingsResponseDto updatePaymentSettings(
      Long id,
      CreateUpdatePaymentSettingsDto createUpdatePaymentSettingsDto) {
    return paymentSettingsService.updatePaymentSettings(id, createUpdatePaymentSettingsDto);
  }

  @Override
  public PaymentSettingsResponseDto getPaymentSettingsByBillingMonth(String billingMonth) {
    return paymentSettingsService.getPaymentSettings(billingMonth);
  }
}
