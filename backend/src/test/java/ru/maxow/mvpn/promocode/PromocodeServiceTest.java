package ru.maxow.mvpn.promocode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.maxow.mvpn.model.CreatePromocodeRequestDto;
import ru.maxow.mvpn.model.PageListPromocodeDto;
import ru.maxow.mvpn.model.PromocodeResponseDto;
import ru.maxow.mvpn.model.PromocodeStatsDto;
import ru.maxow.mvpn.model.PromocodeStatus;
import ru.maxow.mvpn.model.TariffPlanResponseDto;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.tariff.TariffRepository;
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

  @Mock
  private TariffRepository tariffRepository;

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
      request.setTariffId(2L);

      Tariff tariff = new Tariff();
      tariff.setId(2L);

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
      TariffPlanResponseDto tariffDto = new TariffPlanResponseDto();
      tariffDto.setId(2L);
      response.setTariff(tariffDto);
      response.setExpirationDate(saved.getExpirationDate());

      when(tariffRepository.findById(2L)).thenReturn(Optional.of(tariff));
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
      verify(tariffRepository).findById(2L);

      verify(promocodeRepository).save(argThat(entity ->
          entity.getCode() != null
              && entity.getCode().length() == 8
              && entity.getTariff() == tariff
              && entity.getUsageLimit().equals(3)
              && entity.getUsage().equals(0)
              && entity.getStatus() == PromocodeStatus.ACTIVE
              && !entity.getExpirationDate().isBefore(before.plusDays(10).minusSeconds(1))
              && !entity.getExpirationDate().isAfter(after.plusDays(10).plusSeconds(1))
      ));
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException, если тариф не найден")
    void shouldThrowNotFoundWhenTariffMissing() {
      CreatePromocodeRequestDto request = new CreatePromocodeRequestDto();
      request.setUsageLimit(2);
      request.setValidDays(5);
      request.setTariffId(999L);

      when(tariffRepository.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> promocodeService.createPromocode(request))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException ex = (NotFoundException) error;
            assertThat(ex.getEntityName()).isEqualTo("Tariff");
            assertThat(ex.getIdentifier()).isEqualTo("999");
          });

      verify(tariffRepository).findById(999L);
      verify(promocodeRepository, never()).save(any());
      verify(promocodeMapper, never()).toDto(any());
    }
  }

  @Nested
  @DisplayName("Получение списка промокодов")
  class GetPromocodesTests {

    @Test
    @DisplayName("Должен вернуть страницу промокодов с пагинацией и сортировкой")
    void shouldReturnPromocodesPage() {
      Promocode second = new Promocode();
      second.setId(2L);
      second.setCode("QWER5678");
      second.setStatus(PromocodeStatus.USED);

      Page<Promocode> page = new PageImpl<>(List.of(promocode, second),
          PageRequest.of(0, 15, Sort.by(Sort.Direction.DESC, "code")),
          42);

      PromocodeResponseDto dto1 = new PromocodeResponseDto();
      dto1.setId(1L);
      dto1.setCode("ABC12345");
      dto1.setStatus(PromocodeStatus.ACTIVE);

      PromocodeResponseDto dto2 = new PromocodeResponseDto();
      dto2.setId(2L);
      dto2.setCode("QWER5678");
      dto2.setStatus(PromocodeStatus.USED);

      when(promocodeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
      when(promocodeMapper.toDto(promocode)).thenReturn(dto1);
      when(promocodeMapper.toDto(second)).thenReturn(dto2);

      PageListPromocodeDto result = promocodeService.getPromocodes(
          0,
          15,
          List.of("code,desc"),
          null,
          "ABC"
      );

      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getContent()).extracting(PromocodeResponseDto::getCode)
          .containsExactly("ABC12345", "QWER5678");
      assertThat(result.getTotalElements()).isEqualTo(42L);
      assertThat(result.getTotalPages()).isEqualTo(3);
      assertThat(result.getSize()).isEqualTo(15);
      assertThat(result.getNumber()).isEqualTo(0);

      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      verify(promocodeRepository).findAll(any(Specification.class), pageableCaptor.capture());

      Pageable pageable = pageableCaptor.getValue();
      assertThat(pageable.getPageNumber()).isEqualTo(0);
      assertThat(pageable.getPageSize()).isEqualTo(15);
      assertThat(pageable.getSort().getOrderFor("code")).isNotNull();
      assertThat(pageable.getSort().getOrderFor("code").isDescending()).isTrue();

      verify(promocodeMapper).toDto(promocode);
      verify(promocodeMapper).toDto(second);
    }
  }

  @Nested
  @DisplayName("Получение статистики промокодов")
  class GetPromocodeStatsTests {

    @Test
    @DisplayName("Должен вернуть общую статистику по промокодам")
    void shouldReturnPromocodeStats() {
      when(promocodeRepository.count()).thenReturn(15L);
      when(promocodeRepository.countByStatus(PromocodeStatus.ACTIVE)).thenReturn(7L);
      when(promocodeRepository.sumUsage()).thenReturn(42L);

      PromocodeStatsDto result = promocodeService.getPromocodeStats();

      assertThat(result).isNotNull();
      assertThat(result.getTotalCodes()).isEqualTo(15L);
      assertThat(result.getActiveCodes()).isEqualTo(7L);
      assertThat(result.getTotalUsages()).isEqualTo(42L);

      verify(promocodeRepository).count();
      verify(promocodeRepository).countByStatus(PromocodeStatus.ACTIVE);
      verify(promocodeRepository).sumUsage();
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
            assertThat(ex.getIdentifier()).isEqualTo("999");
          });

      verify(promocodeRepository).deleteById(999L);
    }
  }
}

