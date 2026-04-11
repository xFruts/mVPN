package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserRepository;
import ru.maxow.mvpn.util.exception.BadRequestException;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
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
  private SubscriptionRepository subscriptionRepository;

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
    User user = user(100L, "ivanov");
    Server server1 = server(1L, "Moscow-1");
    Server server2 = server(2L, "SPB-1");
    Subscription subscription = activeSubscription(user, Set.of(server1, server2));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));
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
    User user = user(200L, "petrov");
    Server server1 = server(1L, "Moscow-1");
    Server server2 = server(2L, "SPB-1");
    Subscription subscription = activeSubscription(user, Set.of(server1, server2));

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(subscription));
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

  @Test
  @DisplayName("Должен выбросить NotFoundException, если у пользователя нет подписки")
  void shouldThrowWhenSubscriptionNotFound() {
    UUID code = UUID.randomUUID();
    User user = user(300L, "sergeev");

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("Subscription for user");
  }

  @Test
  @DisplayName("Должен выбросить BadRequestException, если подписка неактивна или истекла")
  void shouldThrowWhenSubscriptionIsNotActiveOrExpired() {
    UUID code = UUID.randomUUID();
    User user = user(400L, "alexeev");
    Server activeServer = server(7L, "EU-1");

    Subscription expired = new Subscription();
    expired.setUser(user);
    expired.setStatus(SubscriptionStatus.CANCELED);
    expired.setEndDate(OffsetDateTime.now().minusDays(1));
    Tariff tariff = new Tariff();
    tariff.setServers(Set.of(activeServer));
    expired.setTariff(tariff);

    when(userRepository.findByVerificationCode(code)).thenReturn(Optional.of(user));
    when(subscriptionRepository.findFirstByUser_IdOrderByStartDateDesc(user.getId()))
        .thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> configFacade.getSubscriptionConfig(code))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("Subscription is not active or expired");
  }

  private User user(Long id, String fullName) {
    User user = new User();
    user.setId(id);
    user.setFullName(fullName);
    return user;
  }

  private Server server(Long id, String name) {
    Server server = new Server();
    server.setId(id);
    server.setName(name);
    server.setStatus(ServerStatus.ACTIVE);
    return server;
  }

  private Subscription activeSubscription(User user, Set<Server> servers) {
    Tariff tariff = new Tariff();
    tariff.setServers(servers);

    Subscription subscription = new Subscription();
    subscription.setUser(user);
    subscription.setStatus(SubscriptionStatus.ACTIVE);
    subscription.setEndDate(OffsetDateTime.now().plusDays(30));
    subscription.setTariff(tariff);
    return subscription;
  }
}

