package ru.maxow.mvpn.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import ru.maxow.mvpn.util.converter.KeycloakOAuth2UserService;
import ru.maxow.mvpn.util.converter.KeycloakRoleConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

  private final String issuerUri;
  private final String clientId;
  private final ClientRegistrationRepository clientRegistrationRepository;
  private final CorsProperties corsProperties;


  public SecurityConfig(
      @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}") String issuerUri,
      ClientRegistrationRepository clientRegistrationRepository,
      @Value("${app.keycloak.client-id}") String keycloakClientId,
      CorsProperties corsProperties
  ) {
    this.issuerUri = issuerUri;
    this.clientRegistrationRepository = clientRegistrationRepository;
    this.clientId = keycloakClientId;
    this.corsProperties = corsProperties;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) {
    http
        .cors(Customizer.withDefaults())
        .csrf(Customizer.withDefaults())

        .authorizeHttpRequests(auth -> auth
            // Swagger UI и API документация
            .requestMatchers(
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/swagger-resources/**",
                "/webjars/**"
            ).permitAll()
            .requestMatchers("/api/openapi.yaml", "/api/components/**", "/api/paths/**").permitAll()

            // Actuator endpoints
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()

            .requestMatchers("/v1/config/**").permitAll()

            // API endpoints требующие роль VPN_ADMIN
            .requestMatchers("/v1/users/**", "/v1/servers/**", "/v1/tariffs/**").hasRole("VPN_ADMIN")
            .requestMatchers("/v1/subscriptions/**", "/v1/promocodes/**").hasRole("VPN_ADMIN")
            .requestMatchers("/v1/broadcasts/**", "/v1/payment-settings/**").hasRole("VPN_ADMIN")
            .requestMatchers("/v1/payment-verifications/**").hasRole("VPN_ADMIN")

            .anyRequest().authenticated()
        )

        .oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .oidcUserService(keycloakOAuth2UserService())
            )
        )

        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt
                .jwtAuthenticationConverter(jwtAuthenticationConverter())
            )
        )
        .logout(logout -> logout
            .logoutSuccessHandler(oidcLogoutSuccessHandler())
        );

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
    configuration.setAllowedMethods(corsProperties.getAllowedMethods());
    configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
    configuration.setAllowCredentials(corsProperties.isAllowCredentials());

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setPrincipalClaimName("preferred_username");
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter(clientId));
    return converter;
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
  }

  @Bean
  public KeycloakOAuth2UserService keycloakOAuth2UserService() {
    return new KeycloakOAuth2UserService(this.clientId);
  }

  private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler() {
    OidcClientInitiatedLogoutSuccessHandler successHandler =
        new OidcClientInitiatedLogoutSuccessHandler(this.clientRegistrationRepository);
    successHandler.setPostLogoutRedirectUri("{baseUrl}/");
    return successHandler;
  }
}
