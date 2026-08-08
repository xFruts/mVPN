package ru.maxow.mvpn.payment.paymentverification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.CreateUpdatePaymentVerificationDto;
import ru.maxow.mvpn.model.PageListPaymentVerificationDto;
import ru.maxow.mvpn.model.PaymentVerificationResponseDto;
import ru.maxow.mvpn.model.VerificationStatus;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.BadRequestException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

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

  @Mock
  private UserRepository userRepository;

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

      User existingUser = new User();
      existingUser.setId(12L);

      PaymentVerificationResponseDto response = new PaymentVerificationResponseDto();
      response.setId(1L);
      response.setStatus(VerificationStatus.PENDING);

      when(mapper.toEntity(request)).thenReturn(verification);
      when(userRepository.findById(12L)).thenReturn(Optional.of(existingUser));
      when(repository.save(verification)).thenReturn(verification);
      when(mapper.toDto(verification)).thenReturn(response);

      PaymentVerificationResponseDto result = paymentVerificationService.create(request);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(VerificationStatus.PENDING);

      verify(mapper).toEntity(request);
      verify(userRepository).findById(12L);
      verify(repository).save(verification);
      verify(mapper).toDto(verification);

      assertThat(verification.getUser()).isEqualTo(existingUser);
      assertThat(verification.getStatus()).isEqualTo(VerificationStatus.PENDING);
      assertThat(verification.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Должен бросить NotFoundException, если пользователь не найден")
    void shouldThrowNotFoundWhenUserMissingOnCreate() {
      CreateUpdatePaymentVerificationDto request = new CreateUpdatePaymentVerificationDto();
      request.setPaidUntilDate(LocalDate.parse("2026-09-15"));
      request.setUserId(404L);
      request.setPayerFullName("Ivan Ivanov");
      request.setPaidAmount(java.math.BigDecimal.valueOf(500.00));

      when(mapper.toEntity(request)).thenReturn(verification);
      when(userRepository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> paymentVerificationService.create(request))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException ex = (NotFoundException) error;
            assertThat(ex.getEntityName()).isEqualTo("User");
            assertThat(ex.getIdentifier()).isEqualTo("404");
          });

      verify(userRepository).findById(404L);
      verify(repository, never()).save(any());
      verify(mapper, never()).toDto(any());
    }
  }

  @Nested
  @DisplayName("Получение страницы заявок")
  class GetAllAsPageTests {

    @Test
    @DisplayName("Должен вернуть страницу верификаций с маппингом в DTO")
    void shouldReturnPageOfVerifications() {
      PaymentVerification second = new PaymentVerification();
      second.setId(2L);
      second.setPaidUntilDate("2026-10-01");
      second.setStatus(VerificationStatus.PENDING);

      PaymentVerificationResponseDto firstDto = new PaymentVerificationResponseDto();
      firstDto.setId(1L);
      PaymentVerificationResponseDto secondDto = new PaymentVerificationResponseDto();
      secondDto.setId(2L);

      PageImpl<PaymentVerification> page = new PageImpl<>(
          List.of(verification, second),
          PageRequest.of(0, 20),
          2);

      when(repository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);
      when(mapper.toDto(verification)).thenReturn(firstDto);
      when(mapper.toDto(second)).thenReturn(secondDto);

      PageListPaymentVerificationDto result = paymentVerificationService.getAllAsPage(
          0,
          20,
          List.of("createdAt,desc"),
          VerificationStatus.PENDING,
          "Ivan",
          OffsetDateTime.parse("2026-04-01T00:00:00Z"),
          OffsetDateTime.parse("2026-04-30T23:59:59Z"));

      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getTotalElements()).isEqualTo(2);
      assertThat(result.getNumber()).isEqualTo(0);
      assertThat(result.getSize()).isEqualTo(20);

      verify(repository).findAll(any(Specification.class), any(PageRequest.class));
      verify(mapper).toDto(verification);
      verify(mapper).toDto(second);
    }

    @Test
    @DisplayName("Должен использовать сортировку createdAt,desc по умолчанию")
    void shouldUseDefaultCreatedAtDescSortWhenSortMissing() {
      when(repository.findAll(any(Specification.class), any(PageRequest.class)))
          .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

      paymentVerificationService.getAllAsPage(0, 10, null, null, null, null, null);

      ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
      verify(repository).findAll(any(Specification.class), captor.capture());

      var order = captor.getValue().getSort().getOrderFor("createdAt");
      assertThat(order).isNotNull();
      assertThat(order.getDirection().name()).isEqualTo("DESC");
    }

    @Test
    @DisplayName("Должен бросить BadRequestException если createdFrom позже createdTo")
    void shouldThrowBadRequestWhenFromAfterTo() {
      OffsetDateTime from = OffsetDateTime.parse("2026-05-01T00:00:00Z");
      OffsetDateTime to = OffsetDateTime.parse("2026-04-01T00:00:00Z");

      assertThatThrownBy(() -> paymentVerificationService.getAllAsPage(
          0,
          20,
          null,
          null,
          null,
          from,
          to))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("createdFrom must be earlier than createdTo");

      verify(repository, never()).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Должен игнорировать blank fullName и возвращать страницу")
    void shouldIgnoreBlankFullName() {
      when(repository.findAll(any(Specification.class), any(PageRequest.class)))
          .thenReturn(new PageImpl<>(List.of(verification), PageRequest.of(0, 10), 1));
      when(mapper.toDto(verification)).thenReturn(new PaymentVerificationResponseDto());

      PageListPaymentVerificationDto result = paymentVerificationService.getAllAsPage(
          0,
          10,
          null,
          null,
          "   ",
          null,
          null);

      assertThat(result).isNotNull();
      verify(repository).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Должен бросить BadRequestException при неподдерживаемом поле сортировки")
    void shouldRejectUnsupportedSortProperty() {
      assertThatThrownBy(() -> paymentVerificationService.getAllAsPage(
          0,
          10,
          List.of("user.password,asc"),
          null,
          null,
          null,
          null))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("Unsupported sort property");

      verify(repository, never()).findAll(any(Specification.class), any(PageRequest.class));
    }

    @Test
    @DisplayName("Должен применить явную сортировку по разрешённому полю")
    void shouldApplyAllowedExplicitSort() {
      when(repository.findAll(any(Specification.class), any(PageRequest.class)))
          .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

      paymentVerificationService.getAllAsPage(
          0,
          10,
          List.of("status,asc"),
          null,
          null,
          null,
          null);

      ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
      verify(repository).findAll(any(Specification.class), captor.capture());

      var order = captor.getValue().getSort().getOrderFor("status");
      assertThat(order).isNotNull();
      assertThat(order.getDirection().name()).isEqualTo("ASC");
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
            assertThat(ex.getIdentifier()).isEqualTo("404");
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
            assertThat(ex.getIdentifier()).isEqualTo("405");
          });

      verify(repository).findById(405L);
      verify(repository, never()).save(any());
      verify(mapper, never()).toDto(any());
    }
  }
}
