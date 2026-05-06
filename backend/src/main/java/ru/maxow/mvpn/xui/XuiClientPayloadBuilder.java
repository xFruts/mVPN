package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.maxow.mvpn.subscription.Subscription;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;

@Component
class XuiClientPayloadBuilder {
  private static final long BYTES_IN_GIGABYTE = 1024L * 1024L * 1024L;

  private final ObjectMapper objectMapper;

  XuiClientPayloadBuilder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  ObjectNode buildClientPayload(
      User user,
      Subscription subscription,
      Tariff tariff,
      XuiClient existingClient) {
    ObjectNode clientNode = objectMapper.createObjectNode();
    String flow = existingClient != null && StringUtils.hasText(existingClient.getFlow())
        ? existingClient.getFlow()
        : "xtls-rprx-vision";
    String comment = existingClient != null && existingClient.getComment() != null
        ? existingClient.getComment()
        : "";
    int reset = existingClient != null && existingClient.getReset() != null
        ? existingClient.getReset()
        : 0;

    clientNode.put("id", String.valueOf(user.getXuiId()));
    clientNode.put("flow", flow);
    clientNode.put("email", user.getFullName());
    clientNode.put("limitIp", tariff.getMaxDevices());

    // 3x-ui ожидает лимит трафика в байтах
    clientNode.put("totalGB", tariff.getTrafficLimitGb() * BYTES_IN_GIGABYTE);
    clientNode.put("expiryTime", subscription.getEndDate().toInstant().toEpochMilli());
    clientNode.put("enable", true);
    clientNode.put("subId", String.valueOf(user.getXuiSubscription()));
    clientNode.put("comment", comment);
    clientNode.put("reset", reset);
    return clientNode;
  }

  String wrapClientPayload(ObjectNode clientNode) throws JsonProcessingException {
    ObjectNode clientsWrapper = objectMapper.createObjectNode();
    clientsWrapper.set("clients", objectMapper.createArrayNode().add(clientNode));
    return objectMapper.writeValueAsString(clientsWrapper);
  }
}

