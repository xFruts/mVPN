package ru.maxow.mvpn.tariff;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.CreateUpdateRequestTariffPlanDto;
import ru.maxow.mvpn.model.TariffPlanResponseDto;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.ServerRepository;
import ru.maxow.mvpn.server.ServerService;
import ru.maxow.mvpn.util.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TariffService - Unit тесты (бизнес-логика)")
class TariffServiceTest {

  @Mock
  private TariffRepository tariffRepository;

  @Mock
  private TariffMapper tariffMapper;

  @Mock
  private ServerService serverService;

  @Mock
  private ServerRepository serverRepository;

  @InjectMocks
  private TariffServiceImpl tariffService;

  private Tariff testTariff;

  @BeforeEach
  void setUp() {
    testTariff = new Tariff();
    testTariff.setId(1L);
    testTariff.setName("Basic Tariff");
    testTariff.setMaxDevices(8);
    testTariff.setTrafficLimitGb(100);
  }

  @Nested
  @DisplayName("Создание тарифа (POST /v1/tariffs)")
  class CreateTariffTests {

    @Test
    @DisplayName("Должен успешно создать тариф")
    void shouldCreateTariffSuccessfully() {
      CreateUpdateRequestTariffPlanDto request = new CreateUpdateRequestTariffPlanDto();
      request.setName("New Tariff");
      request.setMaxDevices(5);
      request.setTrafficLimitGb(50);
      request.setServerIds(List.of(10L, 11L));

      Server s1 = new Server();
      s1.setId(10L);
      Server s2 = new Server();
      s2.setId(11L);
      Set<Server> servers = Set.of(s1, s2);

      Tariff savedTariff = new Tariff();
      savedTariff.setId(2L);
      savedTariff.setName("New Tariff");
      savedTariff.setMaxDevices(5);
      savedTariff.setTrafficLimitGb(50);
      savedTariff.setServers(servers);

      TariffPlanResponseDto expectedResponse = new TariffPlanResponseDto();
      expectedResponse.setId(2L);
      expectedResponse.setName("New Tariff");
      expectedResponse.setMaxDevices(5);
      expectedResponse.setTrafficLimitGb(50);

      doAnswer(invocation -> {
        CreateUpdateRequestTariffPlanDto dto = invocation.getArgument(0);
        Tariff target = invocation.getArgument(1);
        target.setName(dto.getName());
        target.setMaxDevices(dto.getMaxDevices());
        target.setTrafficLimitGb(dto.getTrafficLimitGb());
        return null;
      }).when(tariffMapper).updateFromDto(eq(request), any(Tariff.class));
      when(serverService.getServersById(request.getServerIds())).thenReturn(servers);
      when(tariffRepository.save(any(Tariff.class))).thenReturn(savedTariff);
      when(tariffMapper.toResponseDto(savedTariff)).thenReturn(expectedResponse);

      TariffPlanResponseDto result = tariffService.createTariffPlan(request);

      assertThat(result)
          .isNotNull()
          .satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(2L);
            assertThat(dto.getName()).isEqualTo("New Tariff");
            assertThat(dto.getMaxDevices()).isEqualTo(5);
            assertThat(dto.getTrafficLimitGb()).isEqualTo(50);
          });

      ArgumentCaptor<Tariff> tariffCaptor = ArgumentCaptor.forClass(Tariff.class);
      verify(tariffRepository).save(tariffCaptor.capture());
      assertThat(tariffCaptor.getValue().getServers()).isEqualTo(servers);

      verify(serverService).getServersById(request.getServerIds());
      verify(tariffMapper).updateFromDto(eq(request), any(Tariff.class));
      verify(tariffMapper).toResponseDto(savedTariff);
    }

    @Test
    @DisplayName("Не должен сохранять тариф, если серверы не найдены")
    void shouldNotSaveTariffWhenServerLookupFails() {
      CreateUpdateRequestTariffPlanDto request = new CreateUpdateRequestTariffPlanDto();
      request.setName("New Tariff");
      request.setMaxDevices(5);
      request.setTrafficLimitGb(50);
      request.setServerIds(List.of(100L));

      doNothing().when(tariffMapper).updateFromDto(eq(request), any(Tariff.class));
      when(serverService.getServersById(request.getServerIds()))
          .thenThrow(new NotFoundException("Server", 100L));

      assertThatThrownBy(() -> tariffService.createTariffPlan(request))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException exception = (NotFoundException) error;
            assertThat(exception.getEntityName()).isEqualTo("Server");
            assertThat(exception.getIdentifier()).isEqualTo(100L);
          });

      verify(tariffMapper).updateFromDto(eq(request), any(Tariff.class));
      verify(serverService).getServersById(request.getServerIds());
      verify(tariffRepository, never()).save(any());
      verify(tariffMapper, never()).toResponseDto(any());
    }
  }

  @Nested
  @DisplayName("Обновление тарифа (PUT /v1/tariffs/{id})")
  class UpdateTariffTests {

    @Test
    @DisplayName("Должен успешно обновить тариф")
    void shouldUpdateTariffSuccessfully() {
      CreateUpdateRequestTariffPlanDto request = new CreateUpdateRequestTariffPlanDto();
      request.setName("Updated Tariff");
      request.setMaxDevices(10);
      request.setTrafficLimitGb(200);
      request.setServerIds(List.of(10L, 11L));

      Server s1 = new Server();
      s1.setId(10L);
      Server s2 = new Server();
      s2.setId(11L);
      Set<Server> servers = Set.of(s1, s2);

      Tariff updatedTariff = new Tariff();
      updatedTariff.setId(1L);
      updatedTariff.setName("Updated Tariff");
      updatedTariff.setMaxDevices(10);
      updatedTariff.setTrafficLimitGb(200);

      TariffPlanResponseDto expectedResponse = new TariffPlanResponseDto();
      expectedResponse.setId(1L);
      expectedResponse.setName("Updated Tariff");
      expectedResponse.setMaxDevices(10);
      expectedResponse.setTrafficLimitGb(200);

      when(tariffRepository.findById(1L)).thenReturn(Optional.of(testTariff));
      when(serverRepository.existsById(10L)).thenReturn(true);
      when(serverRepository.existsById(11L)).thenReturn(true);
      when(serverService.getServersById(request.getServerIds())).thenReturn(servers);
      doAnswer(invocation -> {
        CreateUpdateRequestTariffPlanDto dto = invocation.getArgument(0);
        Tariff target = invocation.getArgument(1);
        target.setName(dto.getName());
        target.setMaxDevices(dto.getMaxDevices());
        target.setTrafficLimitGb(dto.getTrafficLimitGb());
        return null;
      }).when(tariffMapper).updateFromDto(request, testTariff);
      when(tariffRepository.save(testTariff)).thenReturn(updatedTariff);
      when(tariffMapper.toResponseDto(updatedTariff)).thenReturn(expectedResponse);

      TariffPlanResponseDto result = tariffService.updateTariffPlan(1L, request);

      assertThat(result)
          .isNotNull()
          .satisfies(dto -> {
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("Updated Tariff");
            assertThat(dto.getMaxDevices()).isEqualTo(10);
            assertThat(dto.getTrafficLimitGb()).isEqualTo(200);
          });

      verify(tariffRepository).findById(1L);
      verify(tariffMapper).updateFromDto(request, testTariff);
      verify(serverRepository).existsById(10L);
      verify(serverRepository).existsById(11L);
      verify(serverService).getServersById(request.getServerIds());
      verify(tariffRepository).save(testTariff);
      verify(tariffMapper).toResponseDto(updatedTariff);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException если среди serverIds есть несуществующий сервер")
    void shouldThrowNotFoundWhenAnyServerIdDoesNotExist() {
      CreateUpdateRequestTariffPlanDto request = new CreateUpdateRequestTariffPlanDto();
      request.setName("Updated Tariff");
      request.setMaxDevices(10);
      request.setTrafficLimitGb(200);
      request.setServerIds(List.of(10L, 999L));

      when(tariffRepository.findById(1L)).thenReturn(Optional.of(testTariff));
      doNothing().when(tariffMapper).updateFromDto(request, testTariff);
      when(serverRepository.existsById(10L)).thenReturn(true);
      when(serverRepository.existsById(999L)).thenReturn(false);

      assertThatThrownBy(() -> tariffService.updateTariffPlan(1L, request))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Server")
          .hasMessageContaining("999");

      verify(serverService, never()).getServersById(any());
      verify(tariffRepository, never()).save(any());
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при обновлении несуществующего тарифа")
    void shouldThrowNotFoundOnUpdateForNonExistentTariff() {
      CreateUpdateRequestTariffPlanDto request = new CreateUpdateRequestTariffPlanDto();
      request.setName("Nonexistent Tariff");
      request.setMaxDevices(5);
      request.setTrafficLimitGb(50);

      when(tariffRepository.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> tariffService.updateTariffPlan(999L, request))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Tariff")
          .hasMessageContaining("999");

      verify(tariffRepository).findById(999L);
      verify(tariffRepository, never()).save(any());
      verify(tariffMapper, never()).toResponseDto(any());
    }
  }

  @Nested
  @DisplayName("Получение тарифов")
  class GetTariffTests {

    @Test
    @DisplayName("Должен вернуть список всех тарифов")
    void shouldReturnAllTariffs() {
      Tariff secondTariff = new Tariff();
      secondTariff.setId(2L);
      secondTariff.setName("Pro Tariff");
      secondTariff.setMaxDevices(15);
      secondTariff.setTrafficLimitGb(500);

      TariffPlanResponseDto dto1 = new TariffPlanResponseDto();
      dto1.setId(1L);
      dto1.setName("Basic Tariff");

      TariffPlanResponseDto dto2 = new TariffPlanResponseDto();
      dto2.setId(2L);
      dto2.setName("Pro Tariff");

      when(tariffRepository.findAll()).thenReturn(List.of(testTariff, secondTariff));
      when(tariffMapper.toResponseDto(testTariff)).thenReturn(dto1);
      when(tariffMapper.toResponseDto(secondTariff)).thenReturn(dto2);

      List<TariffPlanResponseDto> result = tariffService.getAllTariffPlans();

      assertThat(result)
          .hasSize(2)
          .extracting(TariffPlanResponseDto::getName)
          .containsExactly("Basic Tariff", "Pro Tariff");

      verify(tariffRepository).findAll();
      verify(tariffMapper).toResponseDto(testTariff);
      verify(tariffMapper).toResponseDto(secondTariff);
    }

    @Test
    @DisplayName("Должен вернуть пустой список, если тарифов нет")
    void shouldReturnEmptyListWhenNoTariffs() {
      when(tariffRepository.findAll()).thenReturn(List.of());

      List<TariffPlanResponseDto> result = tariffService.getAllTariffPlans();

      assertThat(result).isEmpty();
      verify(tariffRepository).findAll();
      verify(tariffMapper, never()).toResponseDto(any());
    }

    @Test
    @DisplayName("Должен вернуть тариф по id")
    void shouldReturnTariffById() {
      TariffPlanResponseDto response = new TariffPlanResponseDto();
      response.setId(1L);
      response.setName("Basic Tariff");

      when(tariffRepository.findById(1L)).thenReturn(Optional.of(testTariff));
      when(tariffMapper.toResponseDto(testTariff)).thenReturn(response);

      TariffPlanResponseDto result = tariffService.getTariffPlanById(1L);

      assertThat(result).isNotNull().satisfies(dto -> {
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Basic Tariff");
      });

      verify(tariffRepository).findById(1L);
      verify(tariffMapper).toResponseDto(testTariff);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при получении несуществующего тарифа")
    void shouldThrowNotFoundOnGetByIdForNonExistentTariff() {
      when(tariffRepository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> tariffService.getTariffPlanById(404L))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Tariff")
          .hasMessageContaining("404");

      verify(tariffRepository).findById(404L);
      verify(tariffMapper, never()).toResponseDto(any());
    }
  }

  @Nested
  @DisplayName("Удаление тарифа (DELETE /v1/tariffs/{id})")
  class DeleteTariffTests {

    @Test
    @DisplayName("Должен успешно удалить существующий тариф")
    void shouldDeleteTariffSuccessfully() {
      when(tariffRepository.existsById(1L)).thenReturn(true);
      doNothing().when(tariffRepository).deleteById(1L);

      tariffService.deleteTariffPlan(1L);

      verify(tariffRepository).existsById(1L);
      verify(tariffRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при удалении несуществующего тарифа")
    void shouldThrowNotFoundOnDeleteForNonExistentTariff() {
      when(tariffRepository.existsById(999L)).thenReturn(false);

      assertThatThrownBy(() -> tariffService.deleteTariffPlan(999L))
          .isInstanceOf(NotFoundException.class)
          .hasMessageContaining("Tariff")
          .hasMessageContaining("999");

      verify(tariffRepository).existsById(999L);
      verify(tariffRepository, never()).deleteById(any());
    }
  }
}
