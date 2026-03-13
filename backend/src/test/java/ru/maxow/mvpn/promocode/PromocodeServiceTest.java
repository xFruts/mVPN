package ru.maxow.mvpn.promocode;

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
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.CreatePromocodeRequestDto;
import ru.maxow.mvpn.model.PromocodeResponseDto;
import ru.maxow.mvpn.model.PromocodeStatus;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromocodeService - Unit тесты")
class PromocodeServiceTest {

  @Mock
  private PromocodeRepository promocodeRepository;

  @Mock
  private PromocodeMapper promocodeMapper;

  @InjectMocks
  private PromocodeServiceImpl promocodeService;

  private Promocode promocode;

  @BeforeEach
  void setUp() {
    promocode = new Promocode();
    promocode.setId(1L);
    promocode.setCode("ABC12345");
    promocode.setUsage(0);
    promocode.setUsageLimit(2);
    promocode.setStatus(PromocodeStatus.ACTIVE);
    promocode.setExpirationDate(OffsetDateTime.now().plusDays(7));
  }

  @Nested
  @DisplayName("Создание промокода")
  class CreatePromocodeTests {

    @Test
    @DisplayName("Должен создать промокод и вернуть dto")
    void shouldCreatePromocodeSuccessfully() {
      CreatePromocodeRequestDto request = new CreatePromocodeRequestDto();
      request.setUsageLimit(3);
      request.setValidDays(10);

      Promocode saved = new Promocode();
      saved.setId(10L);
      saved.setCode("ZXCV1234");
      saved.setUsage(0);
      saved.setUsageLimit(3);
      saved.setStatus(PromocodeStatus.ACTIVE);
      saved.setExpirationDate(OffsetDateTime.now().plusDays(10));

      PromocodeResponseDto response = new PromocodeResponseDto();
      response.setId(10L);
      response.setCode("ZXCV1234");
      response.setUsage(0);
      response.setUsageLimit(3);
      response.setStatus(PromocodeStatus.ACTIVE);
      response.setExpirationDate(saved.getExpirationDate());

      when(promocodeRepository.save(any(Promocode.class))).thenReturn(saved);
      when(promocodeMapper.toDto(saved)).thenReturn(response);

      OffsetDateTime before = OffsetDateTime.now();
      PromocodeResponseDto result = promocodeService.createPromocode(request);
      OffsetDateTime after = OffsetDateTime.now();

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(10L);
      assertThat(result.getCode()).isEqualTo("ZXCV1234");
      assertThat(result.getStatus()).isEqualTo(PromocodeStatus.ACTIVE);

      verify(promocodeRepository).save(any(Promocode.class));
      verify(promocodeMapper).toDto(saved);

      verify(promocodeRepository).save(argThat(entity ->
          entity.getCode() != null
              && entity.getCode().length() == 8
              && entity.getUsageLimit().equals(3)
              && entity.getStatus() == PromocodeStatus.ACTIVE
              && !entity.getExpirationDate().isBefore(before.plusDays(10).minusSeconds(1))
              && !entity.getExpirationDate().isAfter(after.plusDays(10).plusSeconds(1))
      ));
    }
  }

  @Nested
  @DisplayName("Получение списка промокодов")
  class GetPromocodesTests {

    @Test
    @DisplayName("Должен вернуть список промокодов")
    void shouldReturnPromocodes() {
      Promocode second = new Promocode();
      second.setId(2L);
      second.setCode("QWER5678");

      PromocodeResponseDto dto1 = new PromocodeResponseDto();
      dto1.setId(1L);
      dto1.setCode("ABC12345");

      PromocodeResponseDto dto2 = new PromocodeResponseDto();
      dto2.setId(2L);
      dto2.setCode("QWER5678");

      when(promocodeRepository.findAll()).thenReturn(List.of(promocode, second));
      when(promocodeMapper.toDto(promocode)).thenReturn(dto1);
      when(promocodeMapper.toDto(second)).thenReturn(dto2);

      List<PromocodeResponseDto> result = promocodeService.getPromocodes();

      assertThat(result).hasSize(2);
      assertThat(result).extracting(PromocodeResponseDto::getCode)
          .containsExactly("ABC12345", "QWER5678");

      verify(promocodeRepository).findAll();
      verify(promocodeMapper).toDto(promocode);
      verify(promocodeMapper).toDto(second);
    }
  }

  @Nested
  @DisplayName("Применение промокода")
  class UsePromocodeTests {

    @Test
    @DisplayName("Должен выбросить NotFoundException, если промокод не найден")
    void shouldThrowNotFoundWhenCodeMissing() {
      when(promocodeRepository.findByCode("MISSING")).thenReturn(Optional.empty());

      assertThatThrownBy(() -> promocodeService.usePromocode("MISSING"))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException ex = (NotFoundException) error;
            assertThat(ex.getEntityName()).isEqualTo("Promocode");
            assertThat(ex.getIdentifier()).isNull();
          });

      verify(promocodeRepository).findByCode("MISSING");
      verify(promocodeRepository, never()).save(any());
      verify(promocodeMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Должен выбросить BadRequestException, если промокод не ACTIVE")
    void shouldThrowBadRequestWhenCodeIsNotActive() {
      promocode.setStatus(PromocodeStatus.USED);
      when(promocodeRepository.findByCode("ABC12345")).thenReturn(Optional.of(promocode));

      assertThatThrownBy(() -> promocodeService.usePromocode("ABC12345"))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("not active");

      verify(promocodeRepository).findByCode("ABC12345");
      verify(promocodeRepository, never()).save(any());
      verify(promocodeMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Должен пометить промокод EXPIRED и выбросить ошибку, если истек")
    void shouldMarkExpiredAndThrowWhenExpired() {
      promocode.setExpirationDate(OffsetDateTime.now().minusMinutes(1));
      when(promocodeRepository.findByCode("ABC12345")).thenReturn(Optional.of(promocode));

      assertThatThrownBy(() -> promocodeService.usePromocode("ABC12345"))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("expired");

      assertThat(promocode.getStatus()).isEqualTo(PromocodeStatus.EXPIRED);
      verify(promocodeRepository).findByCode("ABC12345");
      verify(promocodeRepository).save(promocode);
      verify(promocodeMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Должен пометить промокод USED и выбросить ошибку при превышении лимита")
    void shouldMarkUsedAndThrowWhenUsageLimitReached() {
      promocode.setUsage(2);
      promocode.setUsageLimit(2);
      when(promocodeRepository.findByCode("ABC12345")).thenReturn(Optional.of(promocode));

      assertThatThrownBy(() -> promocodeService.usePromocode("ABC12345"))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("usage limit reached");

      assertThat(promocode.getStatus()).isEqualTo(PromocodeStatus.USED);
      verify(promocodeRepository).findByCode("ABC12345");
      verify(promocodeRepository).save(promocode);
      verify(promocodeMapper, never()).toDto(any());
    }

    @Test
    @DisplayName("Должен увеличить usage и вернуть dto")
    void shouldIncreaseUsageAndReturnDto() {
      promocode.setUsage(0);
      promocode.setUsageLimit(2);

      PromocodeResponseDto response = new PromocodeResponseDto();
      response.setId(1L);
      response.setCode("ABC12345");
      response.setUsage(1);
      response.setUsageLimit(2);
      response.setStatus(PromocodeStatus.ACTIVE);

      when(promocodeRepository.findByCode("ABC12345")).thenReturn(Optional.of(promocode));
      when(promocodeRepository.save(promocode)).thenReturn(promocode);
      when(promocodeMapper.toDto(promocode)).thenReturn(response);

      PromocodeResponseDto result = promocodeService.usePromocode("ABC12345");

      assertThat(result).isNotNull();
      assertThat(promocode.getUsage()).isEqualTo(1);
      assertThat(promocode.getStatus()).isEqualTo(PromocodeStatus.ACTIVE);
      verify(promocodeRepository).findByCode("ABC12345");
      verify(promocodeRepository).save(promocode);
      verify(promocodeMapper).toDto(promocode);
    }

    @Test
    @DisplayName("Должен пометить USED, когда usage достиг usageLimit")
    void shouldMarkUsedWhenUsageReachedLimitAfterIncrement() {
      promocode.setUsage(1);
      promocode.setUsageLimit(2);

      PromocodeResponseDto response = new PromocodeResponseDto();
      response.setId(1L);
      response.setCode("ABC12345");
      response.setUsage(2);
      response.setUsageLimit(2);
      response.setStatus(PromocodeStatus.USED);

      when(promocodeRepository.findByCode("ABC12345")).thenReturn(Optional.of(promocode));
      when(promocodeRepository.save(promocode)).thenReturn(promocode);
      when(promocodeMapper.toDto(promocode)).thenReturn(response);

      PromocodeResponseDto result = promocodeService.usePromocode("ABC12345");

      assertThat(result).isNotNull();
      assertThat(promocode.getUsage()).isEqualTo(2);
      assertThat(promocode.getStatus()).isEqualTo(PromocodeStatus.USED);
      verify(promocodeRepository).findByCode("ABC12345");
      verify(promocodeRepository).save(promocode);
      verify(promocodeMapper).toDto(promocode);
    }
  }

  @Nested
  @DisplayName("Удаление промокода")
  class DeletePromocodeTests {

    @Test
    @DisplayName("Должен удалить промокод")
    void shouldDeletePromocodeSuccessfully() {
      doNothing().when(promocodeRepository).deleteById(1L);

      promocodeService.deletePromocode(1L);

      verify(promocodeRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Должен бросить NotFoundException при ошибке удаления")
    void shouldThrowNotFoundOnDeleteFailure() {
      doThrow(new RuntimeException("db error")).when(promocodeRepository).deleteById(999L);

      assertThatThrownBy(() -> promocodeService.deletePromocode(999L))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException ex = (NotFoundException) error;
            assertThat(ex.getEntityName()).isEqualTo("Promocode");
            assertThat(ex.getIdentifier()).isEqualTo(999L);
          });

      verify(promocodeRepository).deleteById(999L);
    }
  }
}

