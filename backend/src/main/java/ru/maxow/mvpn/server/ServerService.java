package ru.maxow.mvpn.server;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.maxow.mvpn.server.dto.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.server.dto.ListServerDto;
import ru.maxow.mvpn.server.dto.ServerResponseDto;

import java.util.List;
import java.util.Set;

public interface ServerService {
  Page<ListServerDto> getServers(Pageable pageable);

  ServerResponseDto getServer(Long id);

  //For service use only
  Set<Server> getServersById(List<Long> ids);

  ServerResponseDto createServer(CreateUpdateServerRequestDto request);

  ServerResponseDto updateServer(Long id, CreateUpdateServerRequestDto request);

  ServerResponseDto updateServerStatus(Long id, ServerStatus status);

  void deleteServer(Long id);
}
