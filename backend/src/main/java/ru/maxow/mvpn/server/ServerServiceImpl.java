package ru.maxow.mvpn.server;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.maxow.mvpn.server.dto.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.server.dto.ListServerDto;
import ru.maxow.mvpn.server.dto.ServerResponseDto;
import ru.maxow.mvpn.util.exception.NotFoundException;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerServiceImpl implements ServerService {

  ServerRepository serverRepository;
  ServerMapper serverMapper;

  @Override
  public Page<ListServerDto> getServers(Pageable pageable) {
    return serverRepository.findAll(pageable).map(serverMapper::toListDto);
  }

  @Override
  public ServerResponseDto getServer(Long id) {
    return serverRepository
        .findById(id)
        .map(serverMapper::toDto)
        .orElseThrow(() -> new NotFoundException("Server", id));
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
