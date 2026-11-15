package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import ru.maxow.mvpn.xui.dto.XuiClientDto;

/**
 * Данные о трафике клиента, получаемые из 3X-UI.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record XuiClientTraffic(
    @JsonProperty("email")
    String email,

    @JsonProperty("up")
    long upload,

    @JsonProperty("down")
    long download,

    @JsonProperty("total")
    long total,

    @JsonProperty("expiryTime")
    long expiryTime
) {

  public static XuiClientTraffic zero(String email) {
    return new XuiClientTraffic(email, 0L, 0L, 0L, 0L);
  }

  public static XuiClientTraffic from(String clientEmail, XuiClientDto client) {
    if (client == null) {
      return zero(clientEmail);
    }
    long expiry = client.expiryTime() != null ? client.expiryTime() : 0L;
    long up = 0L;
    long down = 0L;
    if (client.traffic() != null) {
      up = client.traffic().up() != null ? client.traffic().up() : 0L;
      down = client.traffic().down() != null ? client.traffic().down() : 0L;
    }
    if (up == 0L && client.up() != null) {
      up = client.up();
    }
    if (down == 0L && client.down() != null) {
      down = client.down();
    }
    return new XuiClientTraffic(clientEmail, up, down, up + down, expiry);
  }
}
