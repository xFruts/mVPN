package ru.maxow.mvpn.server;

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
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.maxow.mvpn.model.*;
import ru.maxow.mvpn.util.exception.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ServerService - Unit тесты (бизнес-логика)")
class ServerServiceTest {

  @Mock
  private ServerRepository serverRepository;

  @Mock
  private ServerMapper serverMapper;

  @InjectMocks
  private ServerServiceImpl serverService;

  private Server testServer;

  @BeforeEach
  void setUp() {
    testServer = new Server();
    testServer.setId(1L);
    testServer.setName("Moscow-1");
    testServer.setLocation("RU-MOW");
    testServer.setIp("1.1.1.1");
    testServer.setStatus(ServerStatus.ACTIVE);
    testServer.setMaxUsers(200);
    testServer.setMaxTraffic(1000);
  }

  @Nested
  @DisplayName("Получение серверов (GET /v1/servers)")
  class GetServersTests {

    @Test
    @DisplayName("Должен вернуть страницу серверов без сортировки, если sort = null")
    void shouldReturnServersPageWithUnsortedWhenSortIsNull() {
      ListServerDto listDto = new ListServerDto();
      listDto.setId(1L);
      listDto.setName("Moscow-1");

      Page<Server> page = new PageImpl<>(
          List.of(testServer),
          PageRequest.of(0, 10, Sort.unsorted()),
          1
      );

      when(serverRepository.findAll(any(Pageable.class))).thenReturn(page);
      when(serverMapper.toListDto(testServer)).thenReturn(listDto);

      PageListServerDto result = serverService.getServers(0, 10, null);

      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().getFirst().getName()).isEqualTo("Moscow-1");
      assertThat(result.getTotalElements()).isEqualTo(1L);
      assertThat(result.getTotalPages()).isEqualTo(1);
      assertThat(result.getSize()).isEqualTo(10);
      assertThat(result.getNumber()).isEqualTo(0);

      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      verify(serverRepository).findAll(pageableCaptor.capture());
      assertThat(pageableCaptor.getValue().getSort().isUnsorted()).isTrue();
      verify(serverMapper).toListDto(testServer);
    }

    @Test
    @DisplayName("Должен применить sort-параметры с desc и asc")
    void shouldApplySortFromRequest() {
      when(serverRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

      serverService.getServers(0, 20, List.of("name,desc", "location,asc"));

      ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
      verify(serverRepository).findAll(pageableCaptor.capture());

      Sort sort = pageableCaptor.getValue().getSort();
      assertThat(sort.getOrderFor("name")).isNotNull();
      assertThat(sort.getOrderFor("name").isDescending()).isTrue();
      assertThat(sort.getOrderFor("location")).isNotNull();
      assertThat(sort.getOrderFor("location").isAscending()).isTrue();
      assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
      assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }
  }

  @Nested
  @DisplayName("Получение сервера по id (GET /v1/servers/{id})")
  class GetServerByIdTests {

    @Test
    @DisplayName("Должен вернуть сервер по id")
    void shouldReturnServerById() {
      GetServerResponseDto dto = new GetServerResponseDto();
      dto.setId(1L);
      dto.setName("Moscow-1");

      when(serverRepository.findById(1L)).thenReturn(Optional.of(testServer));
      when(serverMapper.toGetDto(testServer)).thenReturn(dto);

      GetServerResponseDto result = serverService.getServer(1L);

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getName()).isEqualTo("Moscow-1");

      verify(serverRepository).findById(1L);
      verify(serverMapper).toGetDto(testServer);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException для несуществующего сервера")
    void shouldThrowNotFoundWhenServerIsMissing() {
      when(serverRepository.findById(404L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> serverService.getServer(404L))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException exception = (NotFoundException) error;
            assertThat(exception.getEntityName()).isEqualTo("Server");
            assertThat(exception.getIdentifier()).isEqualTo("404");
          });

      verify(serverRepository).findById(404L);
      verify(serverMapper, never()).toDto(any());
    }
  }

  @Nested
  @DisplayName("Получение серверов по списку id")
  class GetServersByIdsTests {

    @Test
    @DisplayName("Должен вернуть set серверов по id")
    void shouldReturnServersByIds() {
      Set<Server> expected = Set.of(testServer);
      when(serverRepository.findAllByIdIn(List.of(1L))).thenReturn(expected);

      Set<Server> result = serverService.getServersById(List.of(1L));

      assertThat(result).containsExactly(testServer);
      verify(serverRepository).findAllByIdIn(List.of(1L));
    }
  }

  @Nested
  @DisplayName("Создание сервера (POST /v1/servers)")
  class CreateServerTests {

    @Test
    @DisplayName("Должен успешно создать сервер")
    void shouldCreateServerSuccessfully() {
      CreateUpdateServerRequestDto request = new CreateUpdateServerRequestDto();
      request.setName("SPB-1");
      request.setLocation("RU-SPE");
      request.setIp("2.2.2.2");
      request.setStatus(ServerStatus.ACTIVE);
      request.setMaxUsers(300);
      request.setMaxTraffic(1500);

      Server savedServer = new Server();
      savedServer.setId(2L);
      savedServer.setName("SPB-1");

      ServerResponseDto response = new ServerResponseDto();
      response.setId(2L);
      response.setName("SPB-1");

      doAnswer(invocation -> {
        CreateUpdateServerRequestDto dto = invocation.getArgument(0);
        Server target = invocation.getArgument(1);
        target.setName(dto.getName());
        target.setLocation(dto.getLocation());
        target.setIp(dto.getIp());
        target.setStatus(dto.getStatus());
        target.setMaxUsers(dto.getMaxUsers());
        target.setMaxTraffic(dto.getMaxTraffic());
        return null;
      }).when(serverMapper).updateFromDto(eq(request), any(Server.class));
      when(serverRepository.save(any(Server.class))).thenReturn(savedServer);
      when(serverMapper.toDto(savedServer)).thenReturn(response);

      ServerResponseDto result = serverService.createServer(request);

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(2L);
      assertThat(result.getName()).isEqualTo("SPB-1");

      verify(serverMapper).updateFromDto(eq(request), any(Server.class));
      verify(serverRepository).save(any(Server.class));
      verify(serverMapper).toDto(savedServer);
    }
  }

  @Nested
  @DisplayName("Обновление сервера (PUT /v1/servers/{id})")
  class UpdateServerTests {

    @Test
    @DisplayName("Должен успешно обновить сервер")
    void shouldUpdateServerSuccessfully() {
      CreateUpdateServerRequestDto request = new CreateUpdateServerRequestDto();
      request.setName("Moscow-1-updated");
      request.setLocation("RU-MOW");
      request.setIp("1.1.1.2");
      request.setStatus(ServerStatus.MAINTENANCE);
      request.setMaxUsers(250);
      request.setMaxTraffic(1200);

      Server updated = new Server();
      updated.setId(1L);
      updated.setName("Moscow-1-updated");
      updated.setStatus(ServerStatus.MAINTENANCE);

      ServerResponseDto response = new ServerResponseDto();
      response.setId(1L);
      response.setName("Moscow-1-updated");
      response.setStatus(ServerStatus.MAINTENANCE);

      when(serverRepository.findById(1L)).thenReturn(Optional.of(testServer));
      doAnswer(invocation -> {
        CreateUpdateServerRequestDto dto = invocation.getArgument(0);
        Server target = invocation.getArgument(1);
        target.setName(dto.getName());
        target.setStatus(dto.getStatus());
        return null;
      }).when(serverMapper).updateFromDto(request, testServer);
      when(serverRepository.save(testServer)).thenReturn(updated);
      when(serverMapper.toDto(updated)).thenReturn(response);

      ServerResponseDto result = serverService.updateServer(1L, request);

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getName()).isEqualTo("Moscow-1-updated");
      assertThat(result.getStatus()).isEqualTo(ServerStatus.MAINTENANCE);

      verify(serverRepository).findById(1L);
      verify(serverMapper).updateFromDto(request, testServer);
      verify(serverRepository).save(testServer);
      verify(serverMapper).toDto(updated);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при обновлении несуществующего сервера")
    void shouldThrowNotFoundOnUpdateForNonExistentServer() {
      CreateUpdateServerRequestDto request = new CreateUpdateServerRequestDto();
      request.setName("missing");

      when(serverRepository.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> serverService.updateServer(999L, request))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException exception = (NotFoundException) error;
            assertThat(exception.getEntityName()).isEqualTo("Server");
            assertThat(exception.getIdentifier()).isEqualTo("999");
          });

      verify(serverRepository).findById(999L);
      verify(serverRepository, never()).save(any());
      verify(serverMapper, never()).toDto(any());
    }
  }

  @Nested
  @DisplayName("Обновление статуса сервера (PATCH /v1/servers/{id}/status)")
  class UpdateServerStatusTests {

    @Test
    @DisplayName("Должен успешно обновить статус сервера")
    void shouldUpdateServerStatusSuccessfully() {
      Server saved = new Server();
      saved.setId(1L);
      saved.setStatus(ServerStatus.INACTIVE);

      ServerResponseDto response = new ServerResponseDto();
      response.setId(1L);
      response.setStatus(ServerStatus.INACTIVE);

      when(serverRepository.findById(1L)).thenReturn(Optional.of(testServer));
      when(serverRepository.save(testServer)).thenReturn(saved);
      when(serverMapper.toDto(saved)).thenReturn(response);

      ServerResponseDto result = serverService.updateServerStatus(1L, ServerStatus.INACTIVE);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(ServerStatus.INACTIVE);
      assertThat(testServer.getStatus()).isEqualTo(ServerStatus.INACTIVE);

      verify(serverRepository).findById(1L);
      verify(serverRepository).save(testServer);
      verify(serverMapper).toDto(saved);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при обновлении статуса несуществующего сервера")
    void shouldThrowNotFoundWhenUpdateStatusForNonExistentServer() {
      when(serverRepository.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> serverService.updateServerStatus(999L, ServerStatus.ACTIVE))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException exception = (NotFoundException) error;
            assertThat(exception.getEntityName()).isEqualTo("Server");
            assertThat(exception.getIdentifier()).isEqualTo("999");
          });

      verify(serverRepository).findById(999L);
      verify(serverRepository, never()).save(any());
      verify(serverMapper, never()).toDto(any());
    }
  }

  @Nested
  @DisplayName("Удаление сервера (DELETE /v1/servers/{id})")
  class DeleteServerTests {

    @Test
    @DisplayName("Должен успешно удалить сервер")
    void shouldDeleteServerSuccessfully() {
      doNothing().when(serverRepository).deleteById(1L);

      serverService.deleteServer(1L);

      verify(serverRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Должен выбросить NotFoundException при удалении несуществующего сервера")
    void shouldThrowNotFoundOnDeleteForNonExistentServer() {
      doThrow(new EmptyResultDataAccessException(1)).when(serverRepository).deleteById(999L);

      assertThatThrownBy(() -> serverService.deleteServer(999L))
          .isInstanceOf(NotFoundException.class)
          .satisfies(error -> {
            NotFoundException exception = (NotFoundException) error;
            assertThat(exception.getEntityName()).isEqualTo("Server");
            assertThat(exception.getIdentifier()).isEqualTo("999");
          });

      verify(serverRepository).deleteById(999L);
    }
  }
}

