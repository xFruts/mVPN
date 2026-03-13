package ru.maxow.mvpn;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.maxow.mvpn.model.BroadcastRequestDto;

@SpringBootTest
@ActiveProfiles("test")
class MVpnApplicationTests {

  @MockitoBean
  private ClientRegistrationRepository clientRegistrationRepository;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @MockitoBean
  private KafkaTemplate<String, BroadcastRequestDto> kafkaTemplate;

  @Test
  void contextLoads() {
  }
}
