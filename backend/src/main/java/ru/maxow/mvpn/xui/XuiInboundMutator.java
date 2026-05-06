package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.util.exception.XuiUnavailableException;

import java.util.Optional;

@Slf4j
@Component
class XuiInboundMutator {
  private final ObjectMapper objectMapper;

  XuiInboundMutator(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  Optional<XuiClient> findClientInInbound(XuiInboundsResponse.Inbound inbound, User user) {
    try {
      JsonNode settings = objectMapper.readTree(inbound.getSettings());
      JsonNode clients = settings.get("clients");
      if (clients != null && clients.isArray()) {
        String expectedXuiId = String.valueOf(user.getXuiId());
        Optional<XuiClient> byEmail = Optional.empty();
        for (JsonNode clientNode : clients) {
          XuiClient client = objectMapper.treeToValue(clientNode, XuiClient.class);
          if (expectedXuiId.equals(client.getId())) {
            return Optional.of(client);
          }
          if (user.getFullName().equals(client.getEmail())) {
            byEmail = Optional.of(client);
          }
        }
        if (byEmail.isPresent()) {
          log.warn(
              "XUI client matched by email fallback. inboundId={}, userId={}, xuiId={}, email={}",
              inbound.getId(),
              user.getId(),
              user.getXuiId(),
              user.getFullName());
          return byEmail;
        }
      }
    } catch (JsonProcessingException e) {
      throw new XuiUnavailableException("Failed to parse inbound settings", e);
    }
    return Optional.empty();
  }

  void applyClientPayloadToInbound(
      XuiInboundsResponse.Inbound inbound,
      ObjectNode clientPayload,
      XuiClient existingClient) {
    try {
      JsonNode settingsNode = objectMapper.readTree(inbound.getSettings());
      ObjectNode settings = settingsNode != null && settingsNode.isObject()
          ? (ObjectNode) settingsNode
          : objectMapper.createObjectNode();

      JsonNode clientsNode = settings.get("clients");
      ArrayNode clients = clientsNode != null && clientsNode.isArray()
          ? (ArrayNode) clientsNode
          : objectMapper.createArrayNode();

      if (clientsNode == null || !clientsNode.isArray()) {
        settings.set("clients", clients);
      }

      String payloadId = clientPayload.path("id").asText("");
      String payloadEmail = clientPayload.path("email").asText("");
      String existingId = existingClient != null ? existingClient.getId() : null;
      boolean replaced = false;

      for (int i = 0; i < clients.size(); i++) {
        JsonNode node = clients.get(i);
        String nodeId = node.path("id").asText("");
        String nodeEmail = node.path("email").asText("");
        boolean match = (!payloadId.isEmpty() && payloadId.equals(nodeId))
            || (!payloadEmail.isEmpty() && payloadEmail.equals(nodeEmail))
            || (existingId != null && existingId.equals(nodeId));

        if (match) {
          clients.set(i, clientPayload);
          replaced = true;
          break;
        }
      }

      if (!replaced) {
        clients.add(clientPayload);
      }

      inbound.setSettings(objectMapper.writeValueAsString(settings));
    } catch (Exception e) {
      throw new XuiUnavailableException("Failed to update inbound settings after client sync", e);
    }
  }
}

