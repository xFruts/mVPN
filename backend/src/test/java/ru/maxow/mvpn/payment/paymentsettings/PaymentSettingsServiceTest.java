package ru.maxow.mvpn.payment.paymentsettings;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.CreateUpdatePaymentSettingsDto;
import ru.maxow.mvpn.model.PaymentSettingsResponseDto;
import ru.maxow.mvpn.util.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentSettingsService - Unit тесты")
class PaymentSettingsServiceTest {

  @Mock
  private PaymentSettingsRepository paymentSettingsRepository;

  @Mock
  private PaymentSettingsMapper paymentSettingsMapper;

  @InjectMocks
  private PaymentSettingsServiceImpl paymentSettingsService;

  @Nested
  @DisplayName("Получение настроек")
  class GetPaymentSettingsTests {

    @Test
    @DisplayName("Должен вернуть настройки по billingMonth")
    void shouldReturnSettingsByBillingMonth() {
      PaymentSettings entity = new PaymentSettings();
      entity.setId(1L);
      entity.setBillingMonth("2026-09");

      PaymentSettingsResponseDto dto = new PaymentSettingsResponseDto();
      dto.setId(1L);
      dto.setBillingMonth("2026-09");

      when(paymentSettingsRepository.findByBillingMonth("2026-09")).thenReturn(Optional.of(entity));
      when(paymentSettingsMapper.toDto(entity)).thenReturn(dto);

      PaymentSettingsResponseDto result = paymentSettingsService.getPaymentSettings("2026-09");

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getBillingMonth()).isEqualTo("2026-09");

      verify(paymentSettingsRepository).findByBillingMonth("2026-09");
      verify(paymentSettingsMapper).toDto(entity);
    }

    @Test
    @DisplayName("Должен бросить NotFoundException, если месяц не найден")
    void shouldThrowNotFoundWhenMonthMissing() {
      when(paymentSettingsRepository.findByBillingMonth("2026-10")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> paymentSettingsService.getPaymentSettings("2026-10"))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException ex = (NotFoundException) error;
            assertThat(ex.getIdentifier()).isNull();
          });

      verify(paymentSettingsRepository).findByBillingMonth("2026-10");
      verify(paymentSettingsMapper, never()).toDto(any());
    }
  }

  @Nested
  @DisplayName("Создание настроек")
  class CreatePaymentSettingsTests {

    @Test
    @DisplayName("Должен создать и вернуть настройки")
    void shouldCreatePaymentSettings() {
      CreateUpdatePaymentSettingsDto request = new CreateUpdatePaymentSettingsDto();
      request.setBillingMonth("2026-09");
      request.setExpectedAmount(300.0);
      request.setBankName("T-Bank");
      request.setRequisites("1234567890");

      PaymentSettings toSave = new PaymentSettings();
      toSave.setBillingMonth("2026-09");

      PaymentSettings saved = new PaymentSettings();
      saved.setId(10L);
      saved.setBillingMonth("2026-09");

      PaymentSettingsResponseDto response = new PaymentSettingsResponseDto();
      response.setId(10L);
      response.setBillingMonth("2026-09");

      when(paymentSettingsMapper.toEntity(request)).thenReturn(toSave);
      when(paymentSettingsRepository.save(toSave)).thenReturn(saved);
      when(paymentSettingsMapper.toDto(saved)).thenReturn(response);

      PaymentSettingsResponseDto result = paymentSettingsService.createPaymentSettings(request);

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(10L);
      assertThat(result.getBillingMonth()).isEqualTo("2026-09");

      verify(paymentSettingsMapper).toEntity(request);
      verify(paymentSettingsRepository).save(toSave);
      verify(paymentSettingsMapper).toDto(saved);
    }
  }

  @Nested
  @DisplayName("Обновление настроек")
  class UpdatePaymentSettingsTests {

    @Test
    @DisplayName("Должен обновить и вернуть настройки")
    void shouldUpdatePaymentSettings() {
      CreateUpdatePaymentSettingsDto request = new CreateUpdatePaymentSettingsDto();
      request.setBillingMonth("2026-10");
      request.setExpectedAmount(500.0);
      request.setBankName("Sber");
      request.setRequisites("9876543210");

      PaymentSettings existing = new PaymentSettings();
      existing.setId(5L);
      existing.setBillingMonth("2026-09");

      PaymentSettings saved = new PaymentSettings();
      saved.setId(5L);
      saved.setBillingMonth("2026-10");

      PaymentSettingsResponseDto response = new PaymentSettingsResponseDto();
      response.setId(5L);
      response.setBillingMonth("2026-10");

      when(paymentSettingsRepository.findById(5L)).thenReturn(Optional.of(existing));
      when(paymentSettingsRepository.save(existing)).thenReturn(saved);
      when(paymentSettingsMapper.toDto(saved)).thenReturn(response);

      PaymentSettingsResponseDto result = paymentSettingsService.updatePaymentSettings(5L, request);

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(5L);
      assertThat(result.getBillingMonth()).isEqualTo("2026-10");

      verify(paymentSettingsRepository).findById(5L);
      verify(paymentSettingsMapper).updateEntityFromDto(request, existing);
      verify(paymentSettingsRepository).save(existing);
      verify(paymentSettingsMapper).toDto(saved);
    }

    @Test
    @DisplayName("Должен бросить NotFoundException при обновлении отсутствующих настроек")
    void shouldThrowNotFoundOnUpdateWhenMissing() {
      CreateUpdatePaymentSettingsDto request = new CreateUpdatePaymentSettingsDto();
      when(paymentSettingsRepository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> paymentSettingsService.updatePaymentSettings(404L, request))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException ex = (NotFoundException) error;
            assertThat(ex.getEntityName()).isEqualTo("PaymentSettings");
            assertThat(ex.getIdentifier()).isEqualTo("404");
          });

      verify(paymentSettingsRepository).findById(404L);
      verify(paymentSettingsMapper, never()).updateEntityFromDto(any(), any());
      verify(paymentSettingsRepository, never()).save(any());
      verify(paymentSettingsMapper, never()).toDto(any());
    }
  }
}

