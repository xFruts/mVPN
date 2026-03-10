package ru.maxow.mvpn.server;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.model.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.model.PageListServerDto;
import ru.maxow.mvpn.model.ServerResponseDto;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerServiceImpl implements ServerService {

  ServerRepository serverRepository;
  ServerMapper serverMapper;

  @Override
  public PageListServerDto getServers(Integer page, Integer size, List<String> sort) {
    Sort sorting = (sort == null || sort.isEmpty())
        ? Sort.unsorted()
        : Sort.by(sort.stream().map(s -> {
          String[] parts = s.split(",");
          return parts.length == 2 && parts[1].equalsIgnoreCase("desc")
              ? Sort.Order.desc(parts[0])
              : Sort.Order.asc(parts[0]);
        }).toList());

    Page<Server> servers = serverRepository.findAll(PageRequest.of(page, size, sorting));

    return new PageListServerDto()
        .content(servers.getContent().stream().map(serverMapper::toListDto).toList())
        .totalElements(servers.getTotalElements())
        .totalPages(servers.getTotalPages())
        .size(servers.getSize())
        .number(servers.getNumber());
  }

  @Override
  public ServerResponseDto getServer(Long id) {
    return serverRepository
        .findById(id)
        .map(serverMapper::toDto)
        .orElseThrow(() -> new NotFoundException("Server", id));
  }

  @Override
  public Set<Server> getServersById(List<Long> ids) {
    return serverRepository.findAllByIdIn(ids);
  }

  @Override
  public ServerResponseDto createServer(CreateUpdateServerRequestDto request) {
    Server server = new Server();
    serverMapper.updateFromDto(request, server);
    server = serverRepository.save(server);
    return serverMapper.toDto(server);
  }

  @Override
  public ServerResponseDto updateServer(Long id, CreateUpdateServerRequestDto request) {
    Server server = serverRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Server", id));

    serverMapper.updateFromDto(request, server);
    server = serverRepository.save(server);
    return serverMapper.toDto(server);
  }

  @Override
  public ServerResponseDto updateServerStatus(Long id, ServerStatus status) {
    Server server = serverRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Server", id));

    server.setStatus(status);
    server = serverRepository.save(server);
    return serverMapper.toDto(server);
  }

  @Override
  @Transactional
  public void deleteServer(Long id) {
    try {
      serverRepository.deleteById(id);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Server", id);
    }
  }
}
