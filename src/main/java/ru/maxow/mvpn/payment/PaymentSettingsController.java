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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

  /**
   * Retrieves payment settings by ID.
   *
   * @param id the ID of the payment settings
   * @return the payment settings
   */
  @GetMapping("/{id}")
  public PaymentSettings getPaymentSettings(@PathVariable Long id) {
    return paymentSettingsService.getPaymentSettings(id);
  }

  /**
   * Creates new payment settings.
   *
   * @param dto the payment settings data transfer object
   * @return the created payment settings
   */
  @PostMapping
  public ResponseEntity<PaymentSettings> createPaymentSettings(
      @Valid @RequestBody PaymentSettingsRequestDto dto) {
    PaymentSettings createdSettings = paymentSettingsService.createPaymentSettings(dto);
    log.info("Payment settings created: {}", createdSettings);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdSettings);
  }

  /**
   * Updates existing payment settings.
   *
   * @param id  the ID of the payment settings to update
   * @param dto the payment settings data transfer object
   * @return the updated payment settings
   */
  @PutMapping("/{id}")
  public ResponseEntity<PaymentSettings> updatePaymentSettings(
      @PathVariable Long id,
      @RequestBody PaymentSettingsRequestDto dto) {
    PaymentSettings updatedSettings = paymentSettingsService.updatePaymentSettings(id, dto);
    log.info("Payment settings with ID: {} updated: {}", id, updatedSettings);
    return ResponseEntity.ok(updatedSettings);
  }

  /**
   * Deletes payment settings by ID.
   *
   * @param id the ID of the payment settings to delete
   * @return a response entity with no content
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletePaymentSettings(@PathVariable Long id) {
    paymentSettingsService.deletePaymentSettings(id);
    log.info("Payment settings with ID: {} deleted", id);
    return ResponseEntity.noContent().build();
  }
}
