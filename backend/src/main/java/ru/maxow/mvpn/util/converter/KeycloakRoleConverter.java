package ru.maxow.mvpn.util.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private final String keycloakClientId;

  public KeycloakRoleConverter(String keycloakClientId) {
    this.keycloakClientId = keycloakClientId;
  }

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");

    if (resourceAccess == null) {
      return Collections.emptyList();
    }

    Object clientObj = resourceAccess.get(keycloakClientId);
    if (!(clientObj instanceof Map<?, ?> clientMap)) {
      return Collections.emptyList();
    }

    Object rolesObj = clientMap.get("roles");
    if (!(rolesObj instanceof List<?> roles)) {
      return Collections.emptyList();
    }

    return roles.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
        .collect(Collectors.toList());
  }
}
