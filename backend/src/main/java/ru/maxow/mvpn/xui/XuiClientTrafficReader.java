package ru.maxow.mvpn.xui;

import java.nio.charset.StandardCharsets;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.xui.dto.XuiApiResponse;
import ru.maxow.mvpn.xui.dto.XuiClientDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Сервис сбора телеметрии и статистики трафика клиентов в 3X-UI.
 * <p>
 * Отвечает за чтение метрик потребления трафика (Query)
 * и нормализацию ответа панели.
 */
@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class XuiClientTrafficReader {

  /**
   * Ссылка на обобщенный тип ответа API для десериализации дерева JSON трафика.
   */
  static ParameterizedTypeReference<XuiApiResponse<JsonNode>> JSON_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};

  XuiClientApiClient clientApiClient;
  ObjectMapper objectMapper;

  @Autowired
  XuiClientTrafficReader(
      XuiClientApiClient clientApiClient,
      ObjectMapper objectMapper
  ) {
    this.clientApiClient = clientApiClient;
    this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
  }

  public XuiClientTraffic getClientTraffic(
      RestClient restClient,
      String sessionCookie,
      Server server,
      String clientEmail
  ) {
    String encodedEmail = UriUtils.encodePathSegment(clientEmail, StandardCharsets.UTF_8);

    // 1. Стандартный эндпоинт 3X-UI inbounds getClientTraffics
    XuiClientTraffic traffic = tryFetchTraffic(
        restClient, sessionCookie, "/panel/api/inbounds/getClientTraffics/" + encodedEmail,
        clientEmail
    );

    // 2. Резервный эндпоинт единичного getClientTraffic
    if (traffic == null) {
      traffic = tryFetchTraffic(
          restClient, sessionCookie, "/panel/api/inbounds/getClientTraffic/" + encodedEmail,
          clientEmail
      );
    }

    // 3. Резервный эндпоинт /panel/api/clients/traffic
    if (traffic == null) {
      traffic = tryFetchTraffic(
          restClient, sessionCookie, "/panel/api/clients/traffic/" + encodedEmail,
          clientEmail
      );
    }

    // 4. Резервный эндпоинт /panel/api/clients/get
    if (traffic == null) {
      traffic = tryFetchTraffic(
          restClient, sessionCookie, "/panel/api/clients/get/" + encodedEmail,
          clientEmail
      );
    }

    // 5. Запасной запрос через clientApiClient
    if (traffic == null) {
      try {
        XuiClientDto client = clientApiClient.getClient(restClient, sessionCookie, clientEmail);
        if (client != null) {
          traffic = XuiClientTraffic.from(clientEmail, client);
        }
      } catch (Exception e) {
        log.debug(
            "Запасной запрос клиента не удался для email={}: {}",
            clientEmail, e.getMessage()
        );
      }
    }

    if (traffic == null) {
      log.warn(
          "Клиент не найден в 3X-UI при запросе трафика. serverId={}, email={}",
          server.getId(), clientEmail
      );
      return XuiClientTraffic.zero(clientEmail);
    }
    return traffic;
  }

  private XuiClientTraffic tryFetchTraffic(
      RestClient restClient,
      String sessionCookie,
      String uri,
      String clientEmail
  ) {
    try {
      RestClient.RequestHeadersSpec<?> request = restClient.get().uri(uri);
      if (sessionCookie != null) {
        request = request.header(HttpHeaders.COOKIE, sessionCookie);
      }
      XuiApiResponse<JsonNode> response = request
          .accept(MediaType.APPLICATION_JSON)
          .retrieve()
          .body(JSON_RESPONSE_TYPE);

      if (response == null || !response.success() || response.obj() == null) {
        return null;
      }
      return parseTrafficNode(response.obj(), clientEmail);
    } catch (RestClientResponseException e) {
      log.debug("Эндпоинт {} вернул ошибку {}: {}", uri, e.getStatusCode(),
          e.getResponseBodyAsString());
      return null;
    } catch (Exception e) {
      log.debug("Запрос к {} завершился ошибкой: {}", uri, e.getMessage());
      return null;
    }
  }

  XuiClientTraffic parseTrafficNode(JsonNode node, String clientEmail) {
    if (node == null || node.isNull()) {
      return null;
    }

    if (node.isString()) {
      String text = node.asString().trim();
      if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
        return null;
      }
      try {
        JsonNode parsed = objectMapper.readTree(text);
        return parseTrafficNode(parsed, clientEmail);
      } catch (Exception ignored) {
        log.debug(
            "Не удалось распарсить текстовый ответ трафика: {}",
            text
        );
        return null;
      }
    }

    if (node.isArray()) {
      long totalUp = 0L;
      long totalDown = 0L;
      long maxTotal = 0L;
      long maxExpiry = 0L;
      boolean hasItems = false;

      for (JsonNode item : node) {
        if (item.isObject()) {
          totalUp += extractUp(item);
          totalDown += extractDown(item);
          maxTotal = Math.max(maxTotal, item.path("total").asLong(0L));
          maxExpiry = Math.max(maxExpiry, item.path("expiryTime").asLong(0L));
          hasItems = true;
        }
      }
      return hasItems
          ? new XuiClientTraffic(clientEmail, totalUp, totalDown, totalUp + totalDown, maxExpiry)
          : null;
    }

    if (node.isObject()) {
      long up = extractUp(node);
      long down = extractDown(node);
      long expiry = node.path("expiryTime").asLong(0L);
      return new XuiClientTraffic(clientEmail, up, down, up + down, expiry);
    }

    return null;
  }

  private long extractUp(JsonNode node) {
    if (node.has("up")) {
      return node.path("up").asLong(0L);
    }
    if (node.has("traffic") && node.path("traffic").has("up")) {
      return node.path("traffic").path("up").asLong(0L);
    }
    return 0L;
  }

  private long extractDown(JsonNode node) {
    if (node.has("down")) {
      return node.path("down").asLong(0L);
    }
    if (node.has("traffic") && node.path("traffic").has("down")) {
      return node.path("traffic").path("down").asLong(0L);
    }
    return 0L;
  }
}
