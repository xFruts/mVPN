package ru.maxow.mvpn.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTestController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, SecurityConfigTest.SecurityTestBeans.class, SecurityConfigTestController.class})
@TestPropertySource(properties = {
    "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://localhost:8081/realms/test",
    "app.keycloak.client-id=mVPN",
    "app.cors.allowed-origins[0]=http://localhost:5173",
    "app.cors.allowed-origins[1]=https://vpn.maxow.ru"
})
@DisplayName("SecurityConfig - integration tests")
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CorsConfigurationSource corsConfigurationSource;

  @Autowired
  private JwtAuthenticationConverter jwtAuthenticationConverter;

  @MockitoBean
  private JwtDecoder jwtDecoder;

  @Test
  @DisplayName("PermitAll endpoints are available without authentication")
  void shouldAllowPermitAllEndpointsWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk());

    mockMvc.perform(get("/v1/config/test"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Admin endpoints require authentication")
  void shouldRequireAuthenticationForAdminEndpoints() throws Exception {
    mockMvc.perform(get("/v1/users/test"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Admin endpoints deny access without VPN_ADMIN role")
  void shouldDenyAdminEndpointsForNonAdminRole() throws Exception {
    mockMvc.perform(get("/v1/users/test").with(user("user").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Admin endpoints allow access for VPN_ADMIN role")
  void shouldAllowAdminEndpointsForVpnAdminRole() throws Exception {
    mockMvc.perform(get("/v1/users/test").with(user("admin").roles("VPN_ADMIN")))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Any non-whitelisted endpoint requires authentication")
  void shouldRequireAuthenticationForOtherEndpoints() throws Exception {
    mockMvc.perform(get("/secured/any"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("Any authenticated user can access non-admin endpoint")
  void shouldAllowAuthenticatedUserForOtherEndpoints() throws Exception {
    mockMvc.perform(get("/secured/any").with(user("user").roles("USER")))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("CORS configuration exposes expected origins methods headers")
  void shouldConfigureCorsPolicy() {
    CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(
        new MockHttpServletRequest("GET", "/v1/users/test")
    );

    assertThat(configuration).isNotNull();
    assertThat(configuration.getAllowedOrigins())
        .containsExactlyInAnyOrder("http://localhost:5173", "https://vpn.maxow.ru");
    assertThat(configuration.getAllowedMethods())
        .containsExactlyInAnyOrder("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    assertThat(configuration.getAllowedHeaders())
        .containsExactlyInAnyOrder("Authorization", "Content-Type", "Accept");
    assertThat(configuration.getAllowCredentials()).isTrue();
  }

  @Test
  @DisplayName("JWT converter uses preferred_username and maps Keycloak client roles")
  void shouldMapJwtPrincipalAndAuthorities() {
    Jwt jwt = new Jwt(
        "token-value",
        Instant.now(),
        Instant.now().plusSeconds(300),
        Map.of("alg", "none"),
        Map.of(
            "preferred_username", "alice",
            "resource_access", Map.of(
                "mVPN", Map.of("roles", List.of("VPN_ADMIN", "SUPPORT"))
            )
        )
    );

    AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);

    assertThat(authentication).isNotNull();
    assertThat(authentication.getName()).isEqualTo("alice");
    assertThat(authentication.getAuthorities())
        .extracting(GrantedAuthority::getAuthority)
        .contains("ROLE_VPN_ADMIN", "ROLE_SUPPORT");
  }

  @TestConfiguration
  static class SecurityTestBeans {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
      ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
          .clientId("test-client")
          .clientSecret("test-secret")
          .issuerUri("http://localhost:8081/realms/test")
          .authorizationUri("http://localhost:8081/realms/test/protocol/openid-connect/auth")
          .tokenUri("http://localhost:8081/realms/test/protocol/openid-connect/token")
          .jwkSetUri("http://localhost:8081/realms/test/protocol/openid-connect/certs")
          .userInfoUri("http://localhost:8081/realms/test/protocol/openid-connect/userinfo")
          .userNameAttributeName("preferred_username")
          .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
          .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
          .scope("openid")
          .build();
      return new InMemoryClientRegistrationRepository(registration);
    }
  }
}

