package ru.maxow.mvpn.payment.paymentverification;

import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PageListPaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;
import ru.maxow.mvpn.model.PaymentVerificationStatsDto;
import ru.maxow.mvpn.model.VerificationStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface PaymentVerificationService {
  PageListPaymentVerificationDto getAllAsPage(
      Integer page,
      Integer size,
      List<String> sort,
      VerificationStatus status,
      String fullName,
      OffsetDateTime createdFrom,
      OffsetDateTime createdTo);

  @Transactional(readOnly = true)
  PaymentVerificationStatsDto getPaymentVerificationStats();

  PaymentVerificationResponseDto create(CreateUpdatePaymentVerificationDto dto);

  PaymentVerificationResponseDto approve(Long id, String adminComment);

  PaymentVerificationResponseDto reject(Long id, String rejectReason);
}
