package ru.maxow.mvpn.payment;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.payment.paymentverification.PaymentVerificationService;
import ru.maxow.mvpn.payment.paymentverification.dto.PaymentVerificationRequestDto;
import ru.maxow.mvpn.payment.paymentverification.dto.PaymentVerificationResponseDto;
import ru.maxow.mvpn.subscription.SubscriptionService;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentApprovalFacade {

  PaymentVerificationService paymentVerificationService;
  SubscriptionService subscriptionService;

  @Transactional
  public PaymentVerificationResponseDto approve(
      Long verificationId,
      PaymentVerificationRequestDto dto) {
    PaymentVerificationResponseDto paymentVerification =
        paymentVerificationService.approve(verificationId, dto.adminComment());
    subscriptionService.extendSubscription(
        paymentVerification.userId(), paymentVerification.billingMonth());
    return paymentVerification;
  }

  @Transactional
  public PaymentVerificationResponseDto reject(
      Long verificationId,
      PaymentVerificationRequestDto dto) {
    return paymentVerificationService.reject(verificationId, dto.adminComment());
  }
}
