package ru.maxow.mvpn.telegram.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.server.ServerRepository;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.subscription.SubscriptionRepository;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficState;
import ru.maxow.mvpn.subscription.traffic.SubscriptionTrafficStateService;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.user.UserService;
import ru.maxow.mvpn.xui.XuiPanelService;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramUserService - unit tests")
class TelegramUserServiceTest {

  @Mock
  private UserService userService;

  @Mock
  private SubscriptionRepository subscriptionRepository;

  @Mock
  private SubscriptionTrafficStateService trafficStateService;

  @Mock
  private ServerRepository serverRepository;

  @Mock
  private XuiPanelService xuiPanelService;

  private TelegramUserService telegramUserService;

  @BeforeEach
  void setUp() {
    telegramUserService = new TelegramUserService(
        userService,
        subscriptionRepository,
        trafficStateService,
        serverRepository,
        xuiPanelService,
        "https://sub.example.com"
    );
  }

  @Test
  @DisplayName("findAuthenticatedUser returns user when telegram ID exists")
  void shouldFindAuthenticatedUser() {
    User user = new User();
    user.setId(10L);
    when(userService.findByTelegramId(12345L)).thenReturn(user);

    Optional<User> result = telegramUserService.findAuthenticatedUser(12345L);

    assertThat(result).contains(user);
  }

  @Test
  @DisplayName("findAuthenticatedUser returns empty when telegram ID is null or missing")
  void shouldReturnEmptyForUnknownUser() {
    assertThat(telegramUserService.findAuthenticatedUser(null)).isEmpty();

    when(userService.findByTelegramId(999L)).thenReturn(null);
    assertThat(telegramUserService.findAuthenticatedUser(999L)).isEmpty();
  }

  @Test
  @DisplayName("findAuthenticatedUser auto-binds user by telegram username when unbound")
  void shouldAutoAuthenticateUserByTelegramUsername() {
    User user = new User();
    user.setId(42L);
    user.setTelegramUsername("xFruts");

    when(userService.findByTelegramId(777L)).thenReturn(null);
    when(userService.findByTelegramUsername("xFruts")).thenReturn(Optional.of(user));

    Optional<User> result = telegramUserService.findAuthenticatedUser(777L, "xFruts");

    assertThat(result).contains(user);
    assertThat(user.getUserTelegramId()).isEqualTo(777L);
    verify(userService).bindTelegramIdToUser(42L, 777L);
  }

  @Test
  @DisplayName("findAuthenticatedUser does not lookup by username when blank or null")
  void shouldNotLookupWhenUsernameBlank() {
    when(userService.findByTelegramId(777L)).thenReturn(null);

    assertThat(telegramUserService.findAuthenticatedUser(777L, "")).isEmpty();
    assertThat(telegramUserService.findAuthenticatedUser(777L, "  ")).isEmpty();
    assertThat(telegramUserService.findAuthenticatedUser(777L, null)).isEmpty();

    verify(userService, never()).findByTelegramUsername(anyString());
  }

  private String anyString() {
    return org.mockito.ArgumentMatchers.anyString();
  }

  @Test
  @DisplayName("authenticateUser succeeds with valid verification code")
  void shouldAuthenticateUser() {
    UUID token = UUID.randomUUID();
    when(userService.checkVerificationCode(token)).thenReturn(true);

    boolean success = telegramUserService.authenticateUser(token, 888L);

    assertThat(success).isTrue();
    verify(userService).updateUserTelegramId(token, 888L);
  }

  @Test
  @DisplayName("authenticateUser fails with invalid verification code")
  void shouldFailAuthenticationWithInvalidCode() {
    UUID token = UUID.randomUUID();
    when(userService.checkVerificationCode(token)).thenReturn(false);

    boolean success = telegramUserService.authenticateUser(token, 888L);

    assertThat(success).isFalse();
  }

  @Test
  @DisplayName("getAvailableServers sorts ACTIVE before MAINTENANCE and INACTIVE")
  void shouldSortServersByStatus() {
    User user = new User();
    user.setId(1L);

    Server inactive = new Server();
    inactive.setName("A-Inactive");
    inactive.setStatus(ServerStatus.INACTIVE);

    Server maintenance = new Server();
    maintenance.setName("B-Maintenance");
    maintenance.setStatus(ServerStatus.MAINTENANCE);

    Server active = new Server();
    active.setName("C-Active");
    active.setStatus(ServerStatus.ACTIVE);

    Tariff tariff = new Tariff();
    tariff.setServers(Set.of(inactive, maintenance, active));

    Subscription sub = new Subscription();
    sub.setTariff(tariff);

    when(subscriptionRepository
        .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(1L))
        .thenReturn(Optional.of(sub));

    List<Server> servers = telegramUserService.getAvailableServers(user);

    assertThat(servers).extracting(Server::getStatus)
        .containsExactly(ServerStatus.ACTIVE, ServerStatus.MAINTENANCE, ServerStatus.INACTIVE);
  }

  @Test
  @DisplayName("buildSubscriptionUrl appends verification code to profile URL")
  void shouldBuildSubscriptionUrl() {
    User user = new User();
    UUID code = UUID.randomUUID();
    user.setVerificationCode(code);

    String url = telegramUserService.buildSubscriptionUrl(user);

    assertThat(url).isEqualTo("https://sub.example.com/" + code);
  }

  @Test
  @DisplayName("getDirectVlessKey returns VLESS string when active server available")
  void shouldReturnDirectVlessKey() {
    User user = new User();
    user.setId(1L);

    Server activeServer = new Server();
    activeServer.setName("NL-Node");
    activeServer.setStatus(ServerStatus.ACTIVE);

    Tariff tariff = new Tariff();
    tariff.setServers(Set.of(activeServer));

    Subscription sub = new Subscription();
    sub.setTariff(tariff);

    when(subscriptionRepository
        .findFirstWithTariffAndServersByUser_IdOrderByStartDateDesc(1L))
        .thenReturn(Optional.of(sub));
    when(xuiPanelService.getVlessConfig(activeServer, user))
        .thenReturn("vless://test-uuid@nl.example.com:443?type=tcp");

    Optional<String> result = telegramUserService.getDirectVlessKey(user);

    assertThat(result).contains("vless://test-uuid@nl.example.com:443?type=tcp");
  }
}
