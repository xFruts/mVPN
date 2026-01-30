package ru.maxow.mvpn.payment.paymentsettings;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.maxow.mvpn.payment.paymentsettings.dto.CreateUpdatePaymentSettingsDto;
import ru.maxow.mvpn.payment.paymentsettings.dto.PaymentSettingsResponseDto;

@Slf4j
@Validated
@RestController
@RequestMapping("v1/payment-settings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentSettingsController {

  PaymentSettingsService paymentSettingsService;

  @PostMapping
  public ResponseEntity<PaymentSettingsResponseDto> createPaymentSettings(
      @Valid @RequestBody CreateUpdatePaymentSettingsDto dto) {
    PaymentSettingsResponseDto responseDto = paymentSettingsService.createPaymentSettings(dto);
    return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<PaymentSettingsResponseDto> updatePaymentSettings(
      @PathVariable("id") Long id,
      @Valid @RequestBody CreateUpdatePaymentSettingsDto dto) {
    PaymentSettingsResponseDto responseDto = paymentSettingsService.updatePaymentSettings(id, dto);
    return new ResponseEntity<>(responseDto, HttpStatus.OK);
  }

  @GetMapping("/{billingMonth}")
  public ResponseEntity<PaymentSettingsResponseDto> getPaymentSettings(
      @PathVariable String billingMonth) {
    PaymentSettingsResponseDto responseDto = paymentSettingsService.getPaymentSettings(billingMonth);
    return new ResponseEntity<>(responseDto, HttpStatus.OK);
  }
}
