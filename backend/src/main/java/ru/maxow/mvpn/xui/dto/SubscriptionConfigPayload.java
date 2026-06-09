package ru.maxow.mvpn.xui.dto;

import org.springframework.http.MediaType;
import ru.maxow.mvpn.server.SubscriptionFormat;

public record SubscriptionConfigPayload(String body, SubscriptionFormat format) {
  public String contentType() {
    return format == SubscriptionFormat.JSON
        ? MediaType.APPLICATION_JSON_VALUE
        : MediaType.TEXT_PLAIN_VALUE;
  }

  public boolean isJson() {
    return format == SubscriptionFormat.JSON;
  }
}

