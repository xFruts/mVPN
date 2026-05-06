package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.maxow.mvpn.server.Server;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

@Slf4j
@Component
class XuiTrafficMapper {
  private final ObjectMapper objectMapper;

  XuiTrafficMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  XuiClientTraffic mapTrafficResponse(String responseBody, Server server, String clientId) {
    try {
      JsonNode response = null;
      if (responseBody != null && !responseBody.isBlank()) {
        response = objectMapper.readTree(responseBody);
      }

      if (response == null || !response.path("success").asBoolean(false)) {
        throw new XuiUnavailableException(
            "XUI returned invalid traffic response for server: " + server.getName());
      }

      JsonNode objArray = response.path("obj");
      if (!objArray.isArray() || objArray.isEmpty()) {
        log.warn("No traffic data found for clientId {} on server {}",
            clientId, server.getId());
        return new XuiClientTraffic(null, null, false, null, 0L, 0L, 0L, 0L, 0L, 0, 0L);
      }

      JsonNode trafficNode = objArray.get(0);
      return objectMapper.treeToValue(trafficNode, XuiClientTraffic.class);
    } catch (XuiUnavailableException e) {
      throw e;
    } catch (Exception e) {
      throw new XuiUnavailableException(
          "Unexpected error while getting traffic: " + server.getName(), e);
    }
  }
}

