package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.maxow.mvpn.server.SubscriptionFormat;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.util.exception.GlobalExceptionHandler;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import ru.maxow.mvpn.xui.config.ConfigController;
import ru.maxow.mvpn.xui.config.ConfigFacade;
import ru.maxow.mvpn.xui.dto.SubscriptionConfigPayload;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = ConfigController.class,
    excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
    }
)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = {
    "vpn.config.profile-title=vpn_test",
    "vpn.config.profile-description=primary profile",
    "vpn.config.support-url=https://t.me/test_support",
    "vpn.config.profile-web-page-url=https://example.com/subscriptions/info",
    "vpn.config.profile-update-interval=12"
})
@DisplayName("ConfigController - WebMvc Tests")
class ConfigControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ConfigFacade configFacade;

  @MockitoBean
  private SubscriptionService subscriptionService;

  private static final String API_URL = "/v1/config/{verificationCode}";

  @Test
  @DisplayName("Should return text config and all profile headers (VLESS)")
  void shouldReturnPlainConfigWithHeaders() throws Exception {
    UUID code = UUID.randomUUID();
    String mockPayload = "YmFzZTY0LWNvbmZpZw==";
    String mockUserInfo = "expire=1798502400";

    given(configFacade.getSubscriptionConfig(code))
        .willReturn(new SubscriptionConfigPayload(mockPayload, SubscriptionFormat.VLESS));
    given(subscriptionService.getSubscriptionInfoForUserByCode(code))
        .willReturn(mockUserInfo);

    mockMvc.perform(get(API_URL, code))
        .andExpect(status().isOk())
        .andExpect(content().string(mockPayload))
        .andExpect(header().string("Content-Type", "text/plain;charset=UTF-8"))
        .andExpect(header().string("profile-title", "vpn_test"))
        .andExpect(header().string("profile-description", "primary profile"))
        .andExpect(header().string("support-url", "https://t.me/test_support"))
        .andExpect(header().string("profile-web-page-url", "https://example.com/subscriptions/info"))
        .andExpect(header().string("profile-update-interval", "12"))
        .andExpect(header().string("subscription-userinfo", mockUserInfo))
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  @DisplayName("Should return JSON config with application/json Content-Type")
  void shouldReturnJsonConfigWithJsonContentType() throws Exception {
    UUID code = UUID.randomUUID();
    String jsonPayload = "{\"format\":\"json\"}";

    given(configFacade.getSubscriptionConfig(code))
        .willReturn(new SubscriptionConfigPayload(jsonPayload, SubscriptionFormat.JSON));
    given(subscriptionService.getSubscriptionInfoForUserByCode(code))
        .willReturn("expire=1798502400");

    mockMvc.perform(get(API_URL, code))
        .andExpect(status().isOk())
        .andExpect(content().string(jsonPayload))
        .andExpect(header().string("Content-Type", "application/json;charset=UTF-8"));
  }

  @Test
  @DisplayName("Should return 404 Not Found when config is missing")
  void shouldReturn404WhenConfigNotFound() throws Exception {
    UUID code = UUID.randomUUID();
    given(configFacade.getSubscriptionConfig(code))
        .willThrow(new NotFoundException("Subscription verification code"));

    mockMvc.perform(get(API_URL, code))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string("Subscription verification code"));
  }

  @Test
  @DisplayName("Should return 503 Service Unavailable when XUI nodes are down")
  void shouldReturn503WhenXuiIsUnavailable() throws Exception {
    UUID code = UUID.randomUUID();
    given(configFacade.getSubscriptionConfig(code))
        .willThrow(new XuiUnavailableException("Connection timeout"));

    mockMvc.perform(get(API_URL, code))
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().string("VPN nodes are temporarily unreachable. Please try again later."));
  }

  @Test
  @DisplayName("Should return 500 Internal Server Error for unhandled exceptions")
  void shouldReturn500ForUnexpectedExceptions() throws Exception {
    UUID code = UUID.randomUUID();
    given(configFacade.getSubscriptionConfig(code))
        .willThrow(new RuntimeException("Database connection failed"));

    mockMvc.perform(get(API_URL, code))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string("Server Error. Please try again later"));
  }
}