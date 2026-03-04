package ru.maxow.mvpn.util.converter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class KeycloakOAuth2UserService extends OidcUserService {

  private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
  private static final String ROLES_CLAIM = "roles";
  private static final String ROLE_PREFIX = "ROLE_";

  private final String clientId;

  public KeycloakOAuth2UserService(String clientId) {
    this.clientId = clientId;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);

    Set<GrantedAuthority> authorities = new LinkedHashSet<>(oidcUser.getAuthorities());
    authorities.addAll(extractCLientRoles(oidcUser));

    log.debug("User {} successfully authenticated with authorities: {}", oidcUser.getName(), authorities);

    return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
  }

  private Collection<? extends GrantedAuthority> extractCLientRoles(OidcUser oidcUser) {
    Map<String, Object> realmAccess = oidcUser.getClaimAsMap(RESOURCE_ACCESS_CLAIM);

    if (realmAccess == null || realmAccess.isEmpty()) {
      log.warn("Claim '{}' is missing. No roles will be mapped.", RESOURCE_ACCESS_CLAIM);
      return Collections.emptyList();
    }

    Object clientResource = realmAccess.get(clientId);
    if (!(clientResource instanceof Map<?, ?> clientMap)) {
      log.warn("Client resource '{}' is missing or invalid in token.", clientId);
      return Collections.emptyList();
    }

    Object rolesObject = clientMap.get(ROLES_CLAIM);
    if (!(rolesObject instanceof Collection<?> roles)) {
      log.warn("Roles array is missing for client '{}'.", clientId);
      return Collections.emptyList();
    }

    return roles.stream()
        .filter(role -> role instanceof String)
        .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
        .collect(Collectors.toSet());
  }
}
