package ru.maxow.mvpn.payment.paymentverification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.PaymentVerificationsApi;
import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationRequestDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;
import ru.maxow.mvpn.payment.PaymentApprovalFacade;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentVerificationController implements PaymentVerificationsApi {

  PaymentApprovalFacade approvalFacade;
  PaymentVerificationService paymentVerificationService;

  @Override
  public PaymentVerificationResponseDto v1PaymentVerificationsPost(
      CreateUpdatePaymentVerificationDto createUpdatePaymentVerificationDto) {
    return paymentVerificationService.create(createUpdatePaymentVerificationDto);
  }

  @Override
  public PaymentVerificationResponseDto v1PaymentVerificationsIdApprovePost(
      Long id,
      PaymentVerificationRequestDto paymentVerificationRequestDto) {
    return approvalFacade.approve(id, paymentVerificationRequestDto);
  }

  @Override
  public PaymentVerificationResponseDto v1PaymentVerificationsIdRejectPost(
      Long id,
      PaymentVerificationRequestDto paymentVerificationRequestDto) {
    return approvalFacade.reject(id, paymentVerificationRequestDto);
  }
}
