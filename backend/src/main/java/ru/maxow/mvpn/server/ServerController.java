package ru.maxow.mvpn.server;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import ru.maxow.mvpn.api.ServersApi;
import ru.maxow.mvpn.model.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.model.PageListServerDto;
import ru.maxow.mvpn.model.ServerResponseDto;
import ru.maxow.mvpn.model.ServerStatus;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerController implements ServersApi {
  ServerService serverService;

  @Override
  public PageListServerDto v1ServersGet(Integer page, Integer size, List<String> sort) {
    return serverService.getServers(page, size, sort);
  }

  @Override
  public ServerResponseDto v1ServersPost(CreateUpdateServerRequestDto createUpdateServerRequestDto) {
    return serverService.createServer(createUpdateServerRequestDto);
  }

  @Override
  public ServerResponseDto v1ServersIdPut(Long id, CreateUpdateServerRequestDto createUpdateServerRequestDto) {
    return serverService.updateServer(id, createUpdateServerRequestDto);
  }

  @Override
  public ServerResponseDto v1ServersIdStatusPatch(Long id, ServerStatus status) {
    return serverService.updateServerStatus(id, status);
  }

  @Override
  public void v1ServersIdDelete(Long id) {
    serverService.deleteServer(id);
  }
}
