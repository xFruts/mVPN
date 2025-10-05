package ru.maxow.mvpn.payment;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing payment settings.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("v1/payment-settings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentSettingsController {

  PaymentSettingsService paymentSettingsService;

  @GetMapping
  public PaymentSettingsResponseDto getPaymentSettings() {
    return paymentSettingsService.getLatestPaymentSettings();
  }

  /**
   * Creates or updates payment settings.
   *
   * @param dto the payment settings data to create or update
   * @return the created or updated payment settings
   */
  @PostMapping
  public ResponseEntity<PaymentSettingsResponseDto> createPaymentSettings(
      @Valid @RequestBody PaymentSettingsRequestDto dto) {
    PaymentSettingsResponseDto createdSettings =
        paymentSettingsService.createOrUpdateLatestPaymentSettings(dto);
    log.info("Payment settings created: {}", createdSettings);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdSettings);
  }

  /**
   * Deletes the latest payment settings.
   *
   * @return a ResponseEntity with no content
   */
  @DeleteMapping
  public ResponseEntity<Void> deletePaymentSettings() {
    paymentSettingsService.deleteLatestPaymentSettings();
    log.info("Payment settings deleted");
    return ResponseEntity.noContent().build();
  }
}
