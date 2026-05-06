package ru.maxow.mvpn.xui;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.maxow.mvpn.server.SubscriptionFormat;
import ru.maxow.mvpn.subscription.SubscriptionService;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.xui.config.ConfigController;
import ru.maxow.mvpn.xui.config.ConfigFacade;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ConfigController - unit tests")
class ConfigControllerTest {

  private ConfigFacade configFacade;
  private SubscriptionService subscriptionService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    configFacade = mock(ConfigFacade.class);
    subscriptionService = mock(SubscriptionService.class);

    ConfigController controller = new ConfigController(
        configFacade,
        subscriptionService,
        "mVPN",
        "primary profile",
        "https://t.me/mvpn_support",
        "https://vpn.maxow.ru/subscriptions/info",
        "12"
    );

    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  @DisplayName("Should return text config and subscription headers")
  void shouldReturnPlainConfigWithHeaders() throws Exception {
    UUID code = UUID.randomUUID();
    when(configFacade.getSubscriptionConfig(code))
        .thenReturn(new SubscriptionConfigPayload("YmFzZTY0LWNvbmZpZw==", SubscriptionFormat.VLESS));
    when(subscriptionService.getSubscriptionInfoForUserByCode(code)).thenReturn("expire=1798502400");

    mockMvc.perform(get("/v1/config/{verificationCode}", code))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string("YmFzZTY0LWNvbmZpZw=="))
        .andExpect(header().string("profile-title", "mVPN"))
        .andExpect(header().string("profile-description", "primary profile"))
        .andExpect(header().string("support-url", "https://t.me/mvpn_support"))
        .andExpect(header().string("profile-web-page-url", "https://vpn.maxow.ru/subscriptions/info"))
        .andExpect(header().string("profile-update-interval", "12"))
        .andExpect(header().string("subscription-userinfo", "expire=1798502400"))
        .andExpect(header().string("Cache-Control", "no-store"));
  }

  @Test
  @DisplayName("Should return json config with application/json content type")
  void shouldReturnJsonConfigWithJsonContentType() throws Exception {
    UUID code = UUID.randomUUID();
    when(configFacade.getSubscriptionConfig(code))
        .thenReturn(new SubscriptionConfigPayload("{\"format\":\"json\"}", SubscriptionFormat.JSON));
    when(subscriptionService.getSubscriptionInfoForUserByCode(code)).thenReturn("expire=1798502400");

    mockMvc.perform(get("/v1/config/{verificationCode}", code))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().string("{\"format\":\"json\"}"));
  }

  @Test
  @DisplayName("Should return 404 text response when config is missing")
  void shouldReturn404WhenConfigNotFound() throws Exception {
    UUID code = UUID.randomUUID();
    when(configFacade.getSubscriptionConfig(code))
        .thenThrow(new NotFoundException("Config"));

    mockMvc.perform(get("/v1/config/{verificationCode}", code))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
  }
}

