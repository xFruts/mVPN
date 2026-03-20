package ru.maxow.mvpn.xui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.maxow.mvpn.util.exception.GlobalExceptionHandler;
import ru.maxow.mvpn.util.exception.NotFoundException;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ConfigController - Unit тесты")
class ConfigControllerTest {

  private ConfigFacade configFacade;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    configFacade = mock(ConfigFacade.class);
    ConfigController controller = new ConfigController(configFacade);
    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();
  }

  @Test
  @DisplayName("Должен вернуть text/plain с base64-конфигом")
  void shouldReturnPlainTextConfig() throws Exception {
    UUID code = UUID.randomUUID();
    when(configFacade.getSubscriptionConfig(code)).thenReturn("YmFzZTY0LWNvbmZpZw==");

    mockMvc.perform(get("/v1/config/{verificationCode}", code))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string("YmFzZTY0LWNvbmZpZw=="));
  }

  @Test
  @DisplayName("Должен вернуть 404, если фасад выбросил NotFoundException")
  void shouldReturn404WhenConfigNotFound() throws Exception {
    UUID code = UUID.randomUUID();
    when(configFacade.getSubscriptionConfig(code)).thenThrow(new NotFoundException("Config"));

    mockMvc.perform(get("/v1/config/{verificationCode}", code))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("Должен вернуть 503, если фасад выбросил XuiUnavailableException")
  void shouldReturn503WhenXuiUnavailable() throws Exception {
    UUID code = UUID.randomUUID();
    when(configFacade.getSubscriptionConfig(code))
        .thenThrow(new XuiUnavailableException("xui unavailable"));

    mockMvc.perform(get("/v1/config/{verificationCode}", code))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.errorCode").value("XUI_UNAVAILABLE"))
        .andExpect(jsonPath("$.message").value("XUI service is temporarily unavailable"))
        .andExpect(jsonPath("$.correlationId").isString());
  }
}

