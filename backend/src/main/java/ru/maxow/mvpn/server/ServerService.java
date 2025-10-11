package ru.maxow.mvpn.server;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.maxow.mvpn.server.dto.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.server.dto.ListServerDto;
import ru.maxow.mvpn.server.dto.ServerResponseDto;

public interface ServerService {
  Page<ListServerDto> getServers(Pageable pageable);

  ServerResponseDto getServer(Long id);

  ServerResponseDto createServer(CreateUpdateServerRequestDto request);

  ServerResponseDto updateServer(Long id, CreateUpdateServerRequestDto request);

  ServerResponseDto updateServerStatus(Long id, ServerStatus status);

  void deleteServer(Long id);
}
