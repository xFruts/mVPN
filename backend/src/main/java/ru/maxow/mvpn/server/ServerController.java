package ru.maxow.mvpn.server;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.maxow.mvpn.server.dto.CreateUpdateServerRequestDto;
import ru.maxow.mvpn.server.dto.ListServerDto;
import ru.maxow.mvpn.server.dto.ServerResponseDto;

@Slf4j
@Validated
@RestController
@RequestMapping("v1/servers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ServerController {
  ServerService serverService;

  @GetMapping
  ResponseEntity<Page<ListServerDto>> getServers(Pageable pageable) {
    return ResponseEntity.ok(serverService.getServers(pageable));
  }

  @PostMapping
  ResponseEntity<ServerResponseDto> createServer(
      @Valid @RequestBody CreateUpdateServerRequestDto serverDto) {
    ServerResponseDto createdServer = serverService.createServer(serverDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdServer);
  }

  @PutMapping("/{id}")
  ResponseEntity<ServerResponseDto> updateServer(
      @PathVariable Long id,
      @Valid @RequestBody CreateUpdateServerRequestDto serverDto) {
    ServerResponseDto updatedServer = serverService.updateServer(id, serverDto);
    return ResponseEntity.ok(updatedServer);
  }

  @PatchMapping("/{id}/status")
  ResponseEntity<ServerResponseDto> updateServerStatus(
      @PathVariable Long id,
      @RequestParam ServerStatus status) {
    ServerResponseDto updatedServer = serverService.updateServerStatus(id, status);
    return ResponseEntity.ok(updatedServer);
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> deleteServer(@PathVariable Long id) {
    serverService.deleteServer(id);
    return ResponseEntity.noContent().build();
  }
}
