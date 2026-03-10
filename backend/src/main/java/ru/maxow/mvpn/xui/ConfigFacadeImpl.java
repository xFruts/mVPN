package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.ServerRepository;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.NotFoundException;

import java.util.Base64;
import java.util.List;
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
  public String getSubscriptionConfig(UUID verificationCode) {
    User user = userRepository.findByVerificationCode(verificationCode)
        .orElseThrow(() -> new NotFoundException("User by verification code"));

    List<String> configs = serverRepository.findAllByStatus(ServerStatus.ACTIVE).stream()
        .map(server -> {
          try {
            return xuiPanelService.getVlessConfig(server, user);
          } catch (Exception e) {
            log.warn("Could not get config for server {} and user {}: {}",
                server.getName(), user.getFullName(), e.getMessage());
            return null;
          }
        })
        .filter(java.util.Objects::nonNull)
        .toList();

    if (configs.isEmpty()) {
      throw new NotFoundException("No configs found for user");
    }

    String combinedConfigs = String.join("\n", configs);
    return Base64.getEncoder().encodeToString(combinedConfigs.getBytes());
  }
}
