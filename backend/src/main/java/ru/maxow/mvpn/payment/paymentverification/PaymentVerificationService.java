package ru.maxow.mvpn.payment.paymentverification;

import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;

public interface PaymentVerificationService {
  PaymentVerificationResponseDto create(CreateUpdatePaymentVerificationDto dto);

  PaymentVerificationResponseDto approve(Long id, String adminComment);

  PaymentVerificationResponseDto reject(Long id, String rejectReason);
}
