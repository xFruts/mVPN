package ru.maxow.mvpn.payment.paymentverification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;
import ru.maxow.mvpn.model.VerificationStatus;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentVerificationServiceImpl implements PaymentVerificationService {

  PaymentVerificationMapper mapper;
  PaymentVerificationRepository repository;

  @Override
  public PaymentVerificationResponseDto create(CreateUpdatePaymentVerificationDto dto) {
    PaymentVerification verification = mapper.toEntity(dto);
    verification.setStatus(VerificationStatus.PENDING);
    verification.setCurrency(dto.getCurrency());
    verification.setCreatedAt(Instant.now());
    repository.save(verification);
    log.info("Payment verification with id: {} has been created", verification.getId());
    return mapper.toDto(verification);
  }

  @Override
  public PaymentVerificationResponseDto approve(Long id, String adminComment) {
    PaymentVerification verification = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Payment verification", id));
    verification.setStatus(VerificationStatus.APPROVED);
    verification.setVerifiedAt(Instant.now());
    if (adminComment != null && !adminComment.isBlank()) {
      verification.setAdminComment(adminComment);
    }
    repository.save(verification);
    log.info("Payment verification with id: {} has been approved", id);
    return mapper.toDto(verification);
  }

  @Override
  public PaymentVerificationResponseDto reject(Long id, String rejectReason) {
    PaymentVerification verification = repository.findById(id)
        .orElseThrow(() -> new NotFoundException("Payment verification", id));
    verification.setStatus(VerificationStatus.REJECTED);
    verification.setVerifiedAt(Instant.now());
    if (rejectReason != null && !rejectReason.isBlank()) {
      verification.setAdminComment(rejectReason);
    }
    repository.save(verification);
    log.info("Payment verification with id: {} has been rejected", id);
    return mapper.toDto(verification);
  }
}
