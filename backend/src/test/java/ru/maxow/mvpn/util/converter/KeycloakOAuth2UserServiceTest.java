package ru.maxow.mvpn.util.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("KeycloakOAuth2UserService - unit tests")
class KeycloakOAuth2UserServiceTest {

  private static final String CLIENT_ID = "mVPN";

  private final KeycloakOAuth2UserService service = new KeycloakOAuth2UserService(CLIENT_ID);

  @Test
  @DisplayName("Returns empty authorities when resource_access claim is missing")
  void returnsEmptyWhenResourceAccessMissing() {
	OidcUser oidcUser = oidcUserWithClaim(null);

	Collection<? extends GrantedAuthority> authorities = extractClientRoles(oidcUser);

	assertThat(authorities).isEmpty();
  }

  @Test
  @DisplayName("Returns empty authorities when client block is absent")
  void returnsEmptyWhenClientBlockAbsent() {
	OidcUser oidcUser = oidcUserWithClaim(Map.of(
		"another-client", Map.of("roles", List.of("VPN_ADMIN"))
	));

	Collection<? extends GrantedAuthority> authorities = extractClientRoles(oidcUser);

	assertThat(authorities).isEmpty();
  }

  @Test
  @DisplayName("Returns empty authorities when roles are missing")
  void returnsEmptyWhenRolesMissing() {
	OidcUser oidcUser = oidcUserWithClaim(Map.of(
		CLIENT_ID, Map.of("scope", "openid")
	));

	Collection<? extends GrantedAuthority> authorities = extractClientRoles(oidcUser);

	assertThat(authorities).isEmpty();
  }

  @Test
  @DisplayName("Returns empty authorities when roles are not a collection")
  void returnsEmptyWhenRolesNotCollection() {
	OidcUser oidcUser = oidcUserWithClaim(Map.of(
		CLIENT_ID, Map.of("roles", "VPN_ADMIN")
	));

	Collection<? extends GrantedAuthority> authorities = extractClientRoles(oidcUser);

	assertThat(authorities).isEmpty();
  }

  @Test
  @DisplayName("Maps only string roles and ignores invalid role entries")
  void mapsOnlyStringRoles() {
	OidcUser oidcUser = oidcUserWithClaim(Map.of(
		CLIENT_ID, Map.of("roles", List.of("VPN_ADMIN", 7, true, "SUPPORT"))
	));

	Collection<? extends GrantedAuthority> authorities = extractClientRoles(oidcUser);

	assertThat(authorities)
		.extracting(GrantedAuthority::getAuthority)
		.containsExactlyInAnyOrder("ROLE_VPN_ADMIN", "ROLE_SUPPORT");
  }

  private OidcUser oidcUserWithClaim(Map<String, Object> resourceAccessClaim) {
	OidcUser oidcUser = mock(OidcUser.class);
	when(oidcUser.getClaimAsMap("resource_access")).thenReturn(resourceAccessClaim);
	return oidcUser;
  }

  private Collection<? extends GrantedAuthority> extractClientRoles(OidcUser oidcUser) {
	@SuppressWarnings("unchecked")
	Collection<? extends GrantedAuthority> authorities =
		(Collection<? extends GrantedAuthority>) ReflectionTestUtils.invokeMethod(
			service,
			"extractCLientRoles",
			oidcUser
		);
	return authorities;
  }
}

