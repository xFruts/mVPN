package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.ServerRepository;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigFacadeImpl - Unit тесты")
class ConfigFacadeImplTest {

  @Mock
  private XuiPanelService xuiPanelService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private ServerRepository serverRepository;

  @InjectMocks
  private ConfigFacadeImpl configFacade;

  @Test
  @DisplayName("Должен выбросить NotFoundException, если пользователь по verificationCode не найден")
  void shouldThrowWhenUserNotFoundByVerificationCode() {
    UUID code = UUID.randomUUID();
    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(NotFoundException.class)
        .satisfies(error -> {
          NotFoundException ex = (NotFoundException) error;
          assertThat(ex.getEntityName()).isEqualTo("User by verification code");
          assertThat(ex.getIdentifier()).isNull();
        });
  }

  @Test
  @DisplayName("Должен вернуть base64-конфиг, пропуская серверы с ошибками")
  void shouldReturnBase64ConfigAndSkipUnavailableServers() {
    UUID code = UUID.randomUUID();
    User user = user("ivanov");
    Server server1 = server(1L, "Moscow-1");
    Server server2 = server(2L, "SPB-1");

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(serverRepository.findAllByStatus(ServerStatus.ACTIVE)).thenReturn(List.of(server1, server2));
    when(xuiPanelService.getVlessConfig(server1, user))
        .thenThrow(new XuiUnavailableException("xui temporarily unavailable"));
    when(xuiPanelService.getVlessConfig(server2, user)).thenReturn("vless://ok-2");

    String encoded = configFacade.getSubscriptionConfig(code);
    String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);

    assertThat(decoded).isEqualTo("vless://ok-2");
  }

  @Test
  @DisplayName("Должен выбросить NotFoundException, если ни одного конфига не получено")
  void shouldThrowWhenNoConfigsResolved() {
    UUID code = UUID.randomUUID();
    User user = user("petrov");
    Server server1 = server(1L, "Moscow-1");
    Server server2 = server(2L, "SPB-1");

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(serverRepository.findAllByStatus(ServerStatus.ACTIVE)).thenReturn(List.of(server1, server2));
    when(xuiPanelService.getVlessConfig(server1, user))
        .thenThrow(new XuiUnavailableException("xui down"));
    when(xuiPanelService.getVlessConfig(server2, user))
        .thenThrow(new NotFoundException("config missing"));

    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(NotFoundException.class)
        .satisfies(error -> {
          NotFoundException ex = (NotFoundException) error;
          assertThat(ex.getEntityName()).isEqualTo("No configs found for user");
          assertThat(ex.getIdentifier()).isNull();
        });
  }

  private User user(String fullName) {
    User user = new User();
    user.setFullName(fullName);
    return user;
  }

  private Server server(Long id, String name) {
    Server server = new Server();
    server.setId(id);
    server.setName(name);
    return server;
  }
}

