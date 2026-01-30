package ru.maxow.mvpn.payment.paymentverification;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.maxow.mvpn.payment.PaymentApprovalFacade;
import ru.maxow.mvpn.payment.paymentverification.dto.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.payment.paymentverification.dto.PaymentVerificationRequestDto;
import ru.maxow.mvpn.payment.paymentverification.dto.PaymentVerificationResponseDto;

@Slf4j
@Validated
@RestController
@RequestMapping("v1/payment-verifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentVerificationController {

  PaymentApprovalFacade approvalFacade;
  PaymentVerificationService paymentVerificationService;

  @PostMapping
  public ResponseEntity<PaymentVerificationResponseDto> create(
      @Valid @RequestBody CreateUpdatePaymentVerificationDto dto) {
    return new ResponseEntity<>(paymentVerificationService.create(dto), HttpStatus.CREATED);
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<PaymentVerificationResponseDto> approve(
      @PathVariable Long id,
      @RequestBody @Validated PaymentVerificationRequestDto dto) {
    return new ResponseEntity<>(approvalFacade.approve(id, dto), HttpStatus.OK);
  }

  @PostMapping("/{id}/reject")
  public ResponseEntity<PaymentVerificationResponseDto> reject(
      @PathVariable Long id,
      @RequestBody @Validated PaymentVerificationRequestDto dto) {
    return new ResponseEntity<>(approvalFacade.reject(id, dto), HttpStatus.OK);
  }
}
