package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;
import tools.jackson.databind.JsonNode;

/**
 * Ответ {@code GET /panel/api/inbounds/list}.
 * Содержит список inbound'ов, включая settings и streamSettings.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record XuiInboundsResponse(
    boolean success,
    List<Inbound> obj
) {

  public XuiInboundsResponse {
    obj = obj == null ? null : List.copyOf(obj);
  }

  @Override
  public List<Inbound> obj() {
    return obj == null ? null : List.copyOf(obj);
  }

  public boolean hasInbounds() {
    return success && obj != null && !obj.isEmpty();
  }

  public List<Inbound> getValidInboundsOrThrow() {
    if (!hasInbounds()) {
      throw new XuiUnavailableException("Failed to get inbounds or inbounds list is empty");
    }
    return obj();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Inbound(
      int id,
      int port,
      String tag,
      String protocol,
      JsonNode settings,
      String remark,
      JsonNode streamSettings
  ) {

    private static final String CLIENTS_KEY = "clients";
    private static final String ID_KEY = "id";
    private static final String EMAIL_KEY = "email";

    public List<JsonNode> getClients() {
      if (settings == null) {
        return Collections.emptyList();
      }
      JsonNode clientsNode = settings.get(CLIENTS_KEY);
      if (clientsNode == null || !clientsNode.isArray()) {
        return Collections.emptyList();
      }
      List<JsonNode> list = new ArrayList<>();
      for (JsonNode client : clientsNode) {
        list.add(client);
      }
      return Collections.unmodifiableList(list);
    }

    public Optional<JsonNode> findClient(String expectedUuid, String expectedEmail) {
      JsonNode fallback = null;
      for (JsonNode clientNode : getClients()) {
        String nodeId = clientNode.path(ID_KEY).asString("");
        if (expectedUuid != null && expectedUuid.equals(nodeId)) {
          return Optional.of(clientNode);
        }
        String email = clientNode.path(EMAIL_KEY).asString("");
        if (expectedEmail != null && expectedEmail.equals(email)) {
          fallback = clientNode;
        }
      }
      return Optional.ofNullable(fallback);
    }
  }
}
