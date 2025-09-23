package ru.maxow.mvpn.payment;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Validated
@RestController
@RequestMapping("v1/payment-settings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentSettingsController {
  PaymentSettingsService paymentSettingsService;

  @GetMapping("/{id}")
  public PaymentSettings getPaymentSettings(@PathVariable Long id) {
    return paymentSettingsService.getPaymentSettings(id);
  }

  @PostMapping
  public ResponseEntity<PaymentSettings> createPaymentSettings(@RequestBody PaymentSettingsRequestDto dto) {
    PaymentSettings createdSettings = paymentSettingsService.createPaymentSettings(dto);
    log.info("Payment settings created: {}", createdSettings);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdSettings);
  }

  @PutMapping("/{id}")
  public ResponseEntity<PaymentSettings> updatePaymentSettings(
      @PathVariable Long id,
      @RequestBody PaymentSettingsRequestDto dto) {
    PaymentSettings updatedSettings = paymentSettingsService.updatePaymentSettings(id, dto);
    log.info("Payment settings with ID: {} updated: {}", id, updatedSettings);
    return ResponseEntity.ok(updatedSettings);
  }

  @DeleteMapping
  public ResponseEntity<Void> deletePaymentSettings(@PathVariable Long id) {
    paymentSettingsService.deletePaymentSettings(id);
    log.info("Payment settings with ID: {} deleted", id);
    return ResponseEntity.noContent().build();
  }
}
