package ru.maxow.mvpn.xui;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Управление HTTP-сессиями с 3X-UI Panel.
 * <p>
 * Поддерживает два режима аутентификации:
 * <ul>
 *   <li><b>Cookie</b> — логин через {@code POST /login}, session cookie для дальнейших запросов</li>
 *   <li><b>Bearer token</b> — токен из настроек панели, подставляется в {@code Authorization} header</li>
 * </ul>
 * При наличии {@code xuiAuthToken} на сервере логин не выполняется.
 */
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class XuiSessionClient {
  RestClient.Builder restClientBuilder;

  XuiSessionClient(RestClient.Builder restClientBuilder) {
    this.restClientBuilder = restClientBuilder;
  }

  /**
   * Создаёт {@link RestClient} с base URL, включающим {@code webBasePath}.
   */
  RestClient buildPanelClient(Server server) {
    return buildClient(server, buildBaseUrl(server));
  }

  /**
   * Создаёт {@link RestClient} с корневым base URL (без webBasePath).
   */
  RestClient buildRootClient(Server server) {
    return buildClient(server, buildRootBaseUrl(server));
  }

  /**
   * Выполняет логин или возвращает {@code null}, если сервер использует Bearer-аутентификацию.
   *
   * @return строка cookie для подстановки в {@code Cookie} header, или {@code null} при Bearer-auth
   */
  String login(RestClient restClient, Server server) {
    if (StringUtils.hasText(server.getXuiAuthToken())) {
      return null;
    }
    return doLogin(restClient, server.getXuiLogin(), server.getXuiPassword());
  }

  // ── internal ──────────────────────────────────────────────────────────────

  private RestClient buildClient(Server server, String baseUrl) {
    RestClient.Builder builder = restClientBuilder.clone().baseUrl(baseUrl);
    if (StringUtils.hasText(server.getXuiAuthToken())) {
      builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + server.getXuiAuthToken());
    }
    return builder.build();
  }

  private String buildBaseUrl(Server server) {
    String baseUrl = String.format("https://%s:%d", server.getIp(), server.getPort());
    if (StringUtils.hasText(server.getWebBasePath())) {
      baseUrl += "/" + server.getWebBasePath();
    }
    return baseUrl;
  }

  private String buildRootBaseUrl(Server server) {
    return String.format("https://%s:%d", server.getIp(), server.getPort());
  }

  private String doLogin(RestClient restClient, String username, String password) {
    ResponseEntity<Void> response = restClient.post()
        .uri("/login")
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("username", username, "password", password))
        .retrieve()
        .toBodilessEntity();

    List<String> setCookieHeaders = response.getHeaders().get(HttpHeaders.SET_COOKIE);
    if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
      throw new XuiUnavailableException("Login failed: no session cookie returned");
    }

    List<String> cookiePairs = new ArrayList<>();
    for (String header : setCookieHeaders) {
      String pair = header.split(";")[0].trim();
      if (!pair.isEmpty()) {
        cookiePairs.add(pair);
      }
    }

    return String.join("; ", cookiePairs);
  }
}
