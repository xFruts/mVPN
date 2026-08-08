package ru.maxow.mvpn.payment.paymentverification;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PageListPaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;
import ru.maxow.mvpn.model.VerificationStatus;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static ru.maxow.mvpn.payment.paymentverification.PaymentVerificationSpecifications.createdAtFrom;
import static ru.maxow.mvpn.payment.paymentverification.PaymentVerificationSpecifications.createdAtTo;
import static ru.maxow.mvpn.payment.paymentverification.PaymentVerificationSpecifications.fullNameContains;
import static ru.maxow.mvpn.payment.paymentverification.PaymentVerificationSpecifications.hasStatus;
import static ru.maxow.mvpn.util.PaginationUtils.parseSorting;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentVerificationServiceImpl implements PaymentVerificationService {

  private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
      "id",
      "createdAt",
      "status",
      "paidAmount",
      "paidUntilDate",
      "payerFullName",
      "verifiedAt",
      "currency",
      "user.fullName"
  );

  private static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("createdAt"));

  PaymentVerificationMapper mapper;
  PaymentVerificationRepository repository;
  UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public PageListPaymentVerificationDto getAllAsPage(
      Integer page,
      Integer size,
      List<String> sort,
      VerificationStatus status,
      String fullName,
      OffsetDateTime createdFrom,
      OffsetDateTime createdTo) {

    Instant defaultFrom = ZonedDateTime.now(ZoneOffset.UTC)
        .withDayOfMonth(1)
        .toLocalDate()
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant();

    Instant effectiveFrom = createdFrom != null ? createdFrom.toInstant() : defaultFrom;
    Instant effectiveTo = createdTo != null ? createdTo.toInstant() : Instant.now();

    if (effectiveFrom.isAfter(effectiveTo)) {
      throw new BadRequestException("createdFrom must be earlier than createdTo");
    }

    Specification<PaymentVerification> spec = Specification
        .where(createdAtFrom(effectiveFrom))
        .and(createdAtTo(effectiveTo))
        .and(hasStatus(status))
        .and(fullNameContains(fullName));

    Page<PaymentVerification> verifications = repository.findAll(
        spec,
        PageRequest.of(page, size, parseSorting(sort, ALLOWED_SORT_PROPERTIES, DEFAULT_SORT)));

    return new PageListPaymentVerificationDto()
        .content(verifications.getContent().stream().map(mapper::toDto).toList())
        .totalElements(verifications.getTotalElements())
        .totalPages(verifications.getTotalPages())
        .size(verifications.getSize())
        .number(verifications.getNumber());
  }

  @Override
  public PaymentVerificationResponseDto create(CreateUpdatePaymentVerificationDto dto) {
    PaymentVerification verification = mapper.toEntity(dto);
    User user = userRepository.findById(dto.getUserId())
        .orElseThrow(() -> new NotFoundException("User", dto.getUserId()));

    verification.setUser(user);
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
