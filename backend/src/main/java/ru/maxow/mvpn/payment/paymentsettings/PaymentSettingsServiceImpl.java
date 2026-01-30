package ru.maxow.mvpn.payment.paymentsettings;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.payment.paymentsettings.dto.CreateUpdatePaymentSettingsDto;
import ru.maxow.mvpn.payment.paymentsettings.dto.PaymentSettingsResponseDto;
import ru.maxow.mvpn.util.exception.NotFoundException;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentSettingsServiceImpl implements  PaymentSettingsService {

  PaymentSettingsRepository paymentSettingsRepository;
  PaymentSettingsMapper paymentSettingsMapper;

  @Override
  public PaymentSettingsResponseDto getPaymentSettings(String month) {
    PaymentSettings settings = paymentSettingsRepository.findByBillingMonth(month)
        .orElseThrow(() -> new NotFoundException("PaymentSettings"));
    return paymentSettingsMapper.toDto(settings);
  }

  @Override
  public PaymentSettingsResponseDto createPaymentSettings(CreateUpdatePaymentSettingsDto dto) {
    PaymentSettings paymentSettings = paymentSettingsMapper.toEntity(dto);
    paymentSettings = paymentSettingsRepository.save(paymentSettings);
    log.info("Create Payment Settings with id {}", paymentSettings.getId());
    return paymentSettingsMapper.toDto(paymentSettings);
  }

  @Override
  public PaymentSettingsResponseDto updatePaymentSettings(Long id, CreateUpdatePaymentSettingsDto dto) {
    PaymentSettings paymentSettings = paymentSettingsRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("PaymentSettings", id));

    paymentSettingsMapper.updateEntityFromDto(dto, paymentSettings);
    paymentSettings = paymentSettingsRepository.save(paymentSettings);
    log.info("Update Payment Settings with id {}", paymentSettings.getId());
    return paymentSettingsMapper.toDto(paymentSettings);
  }
}
