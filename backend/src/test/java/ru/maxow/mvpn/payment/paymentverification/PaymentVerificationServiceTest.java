package ru.maxow.mvpn.payment.paymentverification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;
import ru.maxow.mvpn.model.VerificationStatus;
import ru.maxow.mvpn.util.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentVerificationService - Unit тесты")
class PaymentVerificationServiceTest {

  @Mock
  private PaymentVerificationMapper mapper;

  @Mock
  private PaymentVerificationRepository repository;

  @InjectMocks
  private PaymentVerificationServiceImpl paymentVerificationService;

  private PaymentVerification verification;

  @BeforeEach
  void setUp() {
    verification = new PaymentVerification();
    verification.setId(1L);
    verification.setPaidUntilDate("2026-09-15");
    verification.setStatus(VerificationStatus.PENDING);
  }

  @Nested
  @DisplayName("Создание заявки")
  class CreateTests {

    @Test
    @DisplayName("Должен создать verification со статусом PENDING")
    void shouldCreateVerificationWithPendingStatus() {
      CreateUpdatePaymentVerificationDto request = new CreateUpdatePaymentVerificationDto();
      request.setPaidUntilDate(LocalDate.parse("2026-09-15"));
      request.setUserId(12L);
      request.setPayerFullName("Ivan Ivanov");
      request.setPaidAmount(java.math.BigDecimal.valueOf(500.00));

      PaymentVerificationResponseDto response = new PaymentVerificationResponseDto();
      response.setId(1L);
      response.setStatus(VerificationStatus.PENDING);

      when(mapper.toEntity(request)).thenReturn(verification);
      when(repository.save(verification)).thenReturn(verification);
      when(mapper.toDto(verification)).thenReturn(response);

      PaymentVerificationResponseDto result = paymentVerificationService.create(request);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(VerificationStatus.PENDING);

      verify(mapper).toEntity(request);
      verify(repository).save(verification);
      verify(mapper).toDto(verification);

      assertThat(verification.getStatus()).isEqualTo(VerificationStatus.PENDING);
      assertThat(verification.getCreatedAt()).isNotNull();
    }
  }

  @Nested
  @DisplayName("Одобрение заявки")
  class ApproveTests {

    @Test
    @DisplayName("Должен одобрить verification и сохранить adminComment")
    void shouldApproveVerificationAndSetComment() {
      PaymentVerificationResponseDto response = new PaymentVerificationResponseDto();
      response.setId(1L);
      response.setStatus(VerificationStatus.APPROVED);
      response.setAdminComment("ok");

      when(repository.findById(1L)).thenReturn(Optional.of(verification));
      when(repository.save(verification)).thenReturn(verification);
      when(mapper.toDto(verification)).thenReturn(response);

      Instant before = Instant.now();
      PaymentVerificationResponseDto result = paymentVerificationService.approve(1L, "ok");
      Instant after = Instant.now();

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(VerificationStatus.APPROVED);

      assertThat(verification.getStatus()).isEqualTo(VerificationStatus.APPROVED);
      assertThat(verification.getAdminComment()).isEqualTo("ok");
      assertThat(verification.getVerifiedAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));

      verify(repository).findById(1L);
      verify(repository).save(verification);
      verify(mapper).toDto(verification);
    }

    @Test
    @DisplayName("Не должен перезаписывать adminComment, если он blank")
    void shouldNotOverrideAdminCommentWhenBlank() {
      verification.setAdminComment("old-comment");

      when(repository.findById(1L)).thenReturn(Optional.of(verification));
      when(repository.save(verification)).thenReturn(verification);
      when(mapper.toDto(verification)).thenReturn(new PaymentVerificationResponseDto());

      paymentVerificationService.approve(1L, "   ");

      assertThat(verification.getAdminComment()).isEqualTo("old-comment");
      assertThat(verification.getStatus()).isEqualTo(VerificationStatus.APPROVED);
      assertThat(verification.getVerifiedAt()).isNotNull();
      verify(repository).save(verification);
    }

    @Test
    @DisplayName("Не должен перезаписывать adminComment, если комментарий null")
    void shouldNotOverrideAdminCommentWhenNull() {
      verification.setAdminComment("keep-comment");

      when(repository.findById(1L)).thenReturn(Optional.of(verification));
      when(repository.save(verification)).thenReturn(verification);
      when(mapper.toDto(verification)).thenReturn(new PaymentVerificationResponseDto());

      paymentVerificationService.approve(1L, null);

      assertThat(verification.getAdminComment()).isEqualTo("keep-comment");
      assertThat(verification.getStatus()).isEqualTo(VerificationStatus.APPROVED);
      assertThat(verification.getVerifiedAt()).isNotNull();
      verify(repository).save(verification);
    }

    @Test
    @DisplayName("Должен бросить NotFoundException при approve отсутствующей заявки")
    void shouldThrowNotFoundWhenApproveMissing() {
      when(repository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> paymentVerificationService.approve(404L, "ok"))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException ex = (NotFoundException) error;
            assertThat(ex.getEntityName()).isEqualTo("Payment verification");
            assertThat(ex.getIdentifier()).isEqualTo(404L);
          });

      verify(repository).findById(404L);
      verify(repository, never()).save(any());
      verify(mapper, never()).toDto(any());
    }
  }

  @Nested
  @DisplayName("Отклонение заявки")
  class RejectTests {

    @Test
    @DisplayName("Должен отклонить verification и сохранить причину")
    void shouldRejectVerificationAndSetReason() {
      PaymentVerificationResponseDto response = new PaymentVerificationResponseDto();
      response.setId(1L);
      response.setStatus(VerificationStatus.REJECTED);
      response.setAdminComment("invalid receipt");

      when(repository.findById(1L)).thenReturn(Optional.of(verification));
      when(repository.save(verification)).thenReturn(verification);
      when(mapper.toDto(verification)).thenReturn(response);

      paymentVerificationService.reject(1L, "invalid receipt");

      assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REJECTED);
      assertThat(verification.getAdminComment()).isEqualTo("invalid receipt");
      assertThat(verification.getVerifiedAt()).isNotNull();

      verify(repository).findById(1L);
      verify(repository).save(verification);
      verify(mapper).toDto(verification);
    }

    @Test
    @DisplayName("Не должен перезаписывать adminComment при reject, если причина blank")
    void shouldNotOverrideAdminCommentWhenRejectReasonBlank() {
      verification.setAdminComment("keep-comment");

      when(repository.findById(1L)).thenReturn(Optional.of(verification));
      when(repository.save(verification)).thenReturn(verification);
      when(mapper.toDto(verification)).thenReturn(new PaymentVerificationResponseDto());

      paymentVerificationService.reject(1L, "   ");

      assertThat(verification.getAdminComment()).isEqualTo("keep-comment");
      assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REJECTED);
      assertThat(verification.getVerifiedAt()).isNotNull();
      verify(repository).save(verification);
    }

    @Test
    @DisplayName("Не должен перезаписывать adminComment при reject, если причина null")
    void shouldNotOverrideAdminCommentWhenRejectReasonNull() {
      verification.setAdminComment("keep-comment");

      when(repository.findById(1L)).thenReturn(Optional.of(verification));
      when(repository.save(verification)).thenReturn(verification);
      when(mapper.toDto(verification)).thenReturn(new PaymentVerificationResponseDto());

      paymentVerificationService.reject(1L, null);

      assertThat(verification.getAdminComment()).isEqualTo("keep-comment");
      assertThat(verification.getStatus()).isEqualTo(VerificationStatus.REJECTED);
      assertThat(verification.getVerifiedAt()).isNotNull();
      verify(repository).save(verification);
    }

    @Test
    @DisplayName("Должен бросить NotFoundException при reject отсутствующей заявки")
    void shouldThrowNotFoundWhenRejectMissing() {
      when(repository.findById(405L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> paymentVerificationService.reject(405L, "no payment"))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException ex = (NotFoundException) error;
            assertThat(ex.getEntityName()).isEqualTo("Payment verification");
            assertThat(ex.getIdentifier()).isEqualTo(405L);
          });

      verify(repository).findById(405L);
      verify(repository, never()).save(any());
      verify(mapper, never()).toDto(any());
    }
  }
}
