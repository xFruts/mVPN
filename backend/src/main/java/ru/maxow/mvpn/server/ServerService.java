package ru.maxow.mvpn.server;

import org.springframework.web.multipart.MultipartFile;
import ru.maxow.mvpn.model.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.model.PageListServerDto;
import ru.maxow.mvpn.model.ServerResponseDto;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.model.UploadSshKeyResponseDto;

import java.util.List;
import java.util.Set;

public interface ServerService {
  PageListServerDto getServers(Integer page, Integer size, List<String> sort);

  ServerResponseDto getServer(Long id);

  // For service use only
  Set<Server> getServersById(List<Long> ids);

  ServerResponseDto createServer(CreateUpdateServerRequestDto request);

  ServerResponseDto updateServer(Long id, CreateUpdateServerRequestDto request);

  ServerResponseDto updateServerStatus(Long id, ServerStatus status);

  UploadSshKeyResponseDto uploadServerSshKey(MultipartFile file);

  void deleteServer(Long id);
}
