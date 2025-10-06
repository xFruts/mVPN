package ru.maxow.mvpn.payment;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
  public PaymentSettingsResponseDto getLatestPaymentSettings() {
    return  paymentSettingsRepository.findTopByOrderByIdDesc()
        .map(paymentSettingsMapper::toResponseDto)
        .orElseThrow(() -> new NotFoundException("No payment settings found."));
  }

  @Override
  @Transactional
  public PaymentSettingsResponseDto createOrUpdateLatestPaymentSettings(
      PaymentSettingsRequestDto paymentSettingsRequestDto) {
    PaymentSettings paymentSettings =
        paymentSettingsRepository
            .findTopByOrderByIdDesc()
            .orElseGet(PaymentSettings::new);
    paymentSettingsMapper.updateEntityFromDto(paymentSettingsRequestDto, paymentSettings);
    PaymentSettings createdPaymentSettings = paymentSettingsRepository.save(paymentSettings);
    return paymentSettingsMapper.toResponseDto(createdPaymentSettings);
  }

  @Override
  @Transactional
  public void deleteLatestPaymentSettings() {
    try {
      PaymentSettings settingsToDelete =
          paymentSettingsRepository
            .findTopByOrderByIdDesc()
            .orElseThrow(() -> new NotFoundException("No payment settings found to delete."));

      paymentSettingsRepository.deleteById(settingsToDelete.getId());
      log.info("Deleted latest payment settings with ID: {}", settingsToDelete.getId());
    } catch (EmptyResultDataAccessException e) {
      log.warn("No payment settings found to delete.");
      throw new NotFoundException("No payment settings found to delete.");
    }
  }
}
