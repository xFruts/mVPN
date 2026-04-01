package ru.maxow.mvpn.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class CorsProperties {

  private List<String> allowedOrigins = new ArrayList<>();
  private List<String> allowedMethods =
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
  private List<String> allowedHeaders =
      List.of("Authorization", "Content-Type", "Accept");
  private boolean allowCredentials = true;

}
