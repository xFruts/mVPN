package ru.maxow.mvpn.configuration;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
@Getter
public class CorsProperties {

  private static final List<String> DEFAULT_ALLOWED_METHODS =
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
  private static final List<String> DEFAULT_ALLOWED_HEADERS =
      List.of("Authorization", "Content-Type", "Accept");

  private final List<String> allowedOrigins;
  private final List<String> allowedMethods;
  private final List<String> allowedHeaders;
  private final boolean allowCredentials;

  public CorsProperties(
      List<String> allowedOrigins,
      List<String> allowedMethods,
      List<String> allowedHeaders,
      Boolean allowCredentials
  ) {
    this.allowedOrigins = allowedOrigins == null ? new ArrayList<>() : List.copyOf(allowedOrigins);
    this.allowedMethods = allowedMethods == null
        ? DEFAULT_ALLOWED_METHODS
        : List.copyOf(allowedMethods);
    this.allowedHeaders = allowedHeaders == null
        ? DEFAULT_ALLOWED_HEADERS
        : List.copyOf(allowedHeaders);
    this.allowCredentials = allowCredentials == null || allowCredentials;
  }

}
