package ru.maxow.mvpn.payment;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.PaymentVerificationRequestDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;
import ru.maxow.mvpn.payment.paymentverification.PaymentVerificationService;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.util.exception.BadRequestException;

@Slf4j
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
        paymentVerificationService.approve(verificationId, dto.getAdminComment());

    var paidUntilDate = paymentVerification.getPaidUntilDate();

    if (paidUntilDate == null) {
      log.error("Missing paidUntilDate for payment verification {}", verificationId);
      throw new BadRequestException("Unable to extend subscription: paid until date is missing");
    }

    try {
      
      subscriptionService.extendSubscription(
          paymentVerification.getUserId(),
          paidUntilDate.toString()
      );
      
      log.info("Subscription extended for user {} after payment approval", 
          paymentVerification.getUserId());
    } catch (BadRequestException e) {
      log.error("Failed to extend subscription after payment approval for user {}",
          paymentVerification.getUserId(), e);
      throw e;
    }

    return paymentVerification;
  }

  @Transactional
  public PaymentVerificationResponseDto reject(
      Long verificationId,
      PaymentVerificationRequestDto dto) {
    return paymentVerificationService.reject(verificationId, dto.getAdminComment());
  }
}
