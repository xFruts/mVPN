package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.maxow.mvpn.server.ServerRepository;
import ru.maxow.mvpn.server.ServerStatus;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigFacadeImpl implements ConfigFacade {

  XuiPanelService xuiPanelService;
  UserRepository userRepository;
  ServerRepository serverRepository;

  @Override
  public Mono<String> getSubscriptionConfig(UUID verificationCode) {
    Mono<User> userMono = Mono.fromCallable(() -> userRepository.findByVerificationCode(verificationCode)
            .orElseThrow(() -> new NotFoundException("User by verification code")))
        .cache();

    return userMono.flatMap(user ->
        Flux.fromIterable(serverRepository.findAllByStatus(ServerStatus.ACTIVE))
            .flatMap(server -> xuiPanelService.getVlessConfig(server, user)
                .onErrorResume(e -> {
                  log.warn("Could not get config for server {} and user {}: {}", server.getName(), user.getFullName(), e.getMessage());
                  return Mono.empty(); // Skip server on error
                }))
            .collectList()
            .flatMap(configs -> {
              if (configs.isEmpty()) {
                return Mono.empty(); // Return empty Mono if no configs found
              }
              String combinedConfigs = String.join("\n", configs);
              return Mono.just(Base64.getEncoder().encodeToString(combinedConfigs.getBytes()));
            })
    );
  }
}
