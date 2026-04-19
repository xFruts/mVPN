package ru.maxow.mvpn.payment.paymentverification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.PaymentVerificationsApi;
import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PageListPaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationRequestDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;
import ru.maxow.mvpn.model.VerificationStatus;
import ru.maxow.mvpn.payment.PaymentApprovalFacade;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentVerificationController implements PaymentVerificationsApi {

  PaymentApprovalFacade approvalFacade;
  PaymentVerificationService paymentVerificationService;

  @Override
  public PageListPaymentVerificationDto getPaymentVerifications(
      Integer page,
      Integer size,
      List<String> sort,
      VerificationStatus status,
      String fullName,
      OffsetDateTime createdFrom,
      OffsetDateTime createdTo) {
    return paymentVerificationService.getAllAsPage(
        page,
        size,
        sort,
        status,
        fullName,
        createdFrom,
        createdTo);
  }

  @Override
  public PaymentVerificationResponseDto createPaymentVerification(
      CreateUpdatePaymentVerificationDto createUpdatePaymentVerificationDto) {
    return paymentVerificationService.create(createUpdatePaymentVerificationDto);
  }

  @Override
  public PaymentVerificationResponseDto approvePaymentVerification(
      Long id,
      PaymentVerificationRequestDto paymentVerificationRequestDto) {
    return approvalFacade.approve(id, paymentVerificationRequestDto);
  }

  @Override
  public PaymentVerificationResponseDto rejectPaymentVerification(
      Long id,
      PaymentVerificationRequestDto paymentVerificationRequestDto) {
    return approvalFacade.reject(id, paymentVerificationRequestDto);
  }
}
