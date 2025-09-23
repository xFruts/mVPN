package ru.maxow.mvpn.payment;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.util.exception.NotFoundException;

/**
 * Service implementation for managing PaymentSettings entities.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentSettingsServiceImpl implements  PaymentSettingsService {

  PaymentSettingsRepository paymentSettingsRepository;
  PaymentSettingsMapper paymentSettingsMapper;

  @Override
  public PaymentSettings getPaymentSettings(Long id) {
    return  paymentSettingsRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("PaymentSettings", id));
  }

  @Override
  public PaymentSettings createPaymentSettings(
      PaymentSettingsRequestDto paymentSettingsRequestDto) {
    PaymentSettings paymentSettings = paymentSettingsMapper.toEntity(paymentSettingsRequestDto);
    log.info("create payment settings with ID: {}", paymentSettings.getId());
    return paymentSettingsRepository.save(paymentSettings);
  }

  @Override
  public PaymentSettings updatePaymentSettings(
      Long id, PaymentSettingsRequestDto paymentSettingsRequestDto) {
    PaymentSettings existingPaymentSettings = getPaymentSettings(id);
    paymentSettingsMapper.updateEntityFromDto(paymentSettingsRequestDto, existingPaymentSettings);
    log.info("update payment settings with ID: {}", existingPaymentSettings.getId());
    return paymentSettingsRepository.save(existingPaymentSettings);
  }

  @Override
  public void deletePaymentSettings(Long id) {
    try {
      paymentSettingsRepository.deleteById(id);
      log.info("delete payment settings with ID: {}", id);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("PaymentSettings", id);
    }
  }
}
