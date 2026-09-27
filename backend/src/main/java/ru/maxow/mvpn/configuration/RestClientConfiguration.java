package ru.maxow.mvpn.configuration;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RestClientConfiguration {

  @Bean
  public RestClient.Builder restClientBuilder(
      @Value("${app.http-client.connect-timeout-ms:5000}") int connectTimeoutMs,
      @Value("${app.http-client.read-timeout-ms:8000}") int readTimeoutMs
  ) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
    requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

    return RestClient.builder().requestFactory(requestFactory);
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
