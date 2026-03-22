package ru.maxow.mvpn.util.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KeycloakRoleConverter - unit tests")
class KeycloakRoleConverterTest {

  private static final String CLIENT_ID = "mVPN";

  private final KeycloakRoleConverter converter = new KeycloakRoleConverter(CLIENT_ID);

  @Test
  @DisplayName("Returns empty authorities when resource_access claim is missing")
  void returnsEmptyWhenResourceAccessMissing() {
    Jwt jwt = jwtWithClaims(Map.of("preferred_username", "user"));

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities).isEmpty();
  }

  @Test
  @DisplayName("Returns empty authorities when client block is missing")
  void returnsEmptyWhenClientBlockMissing() {
    Jwt jwt = jwtWithClaims(Map.of(
        "resource_access", Map.of("another-client", Map.of("roles", List.of("VPN_ADMIN")))
    ));

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities).isEmpty();
  }

  @Test
  @DisplayName("Returns empty authorities when roles field is absent")
  void returnsEmptyWhenRolesMissing() {
    Jwt jwt = jwtWithClaims(Map.of(
        "resource_access", Map.of(CLIENT_ID, Map.of("scope", "openid"))
    ));

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities).isEmpty();
  }

  @Test
  @DisplayName("Returns empty authorities when roles is not a list")
  void returnsEmptyWhenRolesNotList() {
    Jwt jwt = jwtWithClaims(Map.of(
        "resource_access", Map.of(CLIENT_ID, Map.of("roles", "VPN_ADMIN"))
    ));

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities).isEmpty();
  }

  @Test
  @DisplayName("Maps only string roles and ignores invalid role entries")
  void mapsOnlyValidStringRoles() {
    Jwt jwt = jwtWithClaims(Map.of(
        "resource_access", Map.of(CLIENT_ID, Map.of("roles", List.of("VPN_ADMIN", 123, true, "SUPPORT")))
    ));

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities)
        .extracting(GrantedAuthority::getAuthority)
        .containsExactlyInAnyOrder("ROLE_VPN_ADMIN", "ROLE_SUPPORT");
  }

  private Jwt jwtWithClaims(Map<String, Object> claims) {
    return new Jwt(
        "token-value",
        Instant.now(),
        Instant.now().plusSeconds(300),
        Map.of("alg", "none"),
        claims
    );
  }
}

