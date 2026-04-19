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
import ru.maxow.mvpn.model.UploadSshKeyResponseDto;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerController implements ServersApi {
  ServerService serverService;

  @Override
  public PageListServerDto getServers(Integer page, Integer size, List<String> sort) {
    return serverService.getServers(page, size, sort);
  }

  @Override
  public ServerResponseDto createServer(CreateUpdateServerRequestDto createUpdateServerRequestDto) {
    return serverService.createServer(createUpdateServerRequestDto);
  }

  @Override
  public ServerResponseDto updateServer(Long id, CreateUpdateServerRequestDto createUpdateServerRequestDto) {
    return serverService.updateServer(id, createUpdateServerRequestDto);
  }

  @Override
  public ServerResponseDto updateServerStatus(Long id, ServerStatus status) {
    return serverService.updateServerStatus(id, status);
  }

  @Override
  public UploadSshKeyResponseDto uploadServerSshKey(MultipartFile file) {
    return serverService.uploadServerSshKey(file);
  }

  @Override
  public void deleteServer(Long id) {
    serverService.deleteServer(id);
  }

  @Override
  public ServerResponseDto getServerById(Long id) {
    return serverService.getServer(id);
  }
}
