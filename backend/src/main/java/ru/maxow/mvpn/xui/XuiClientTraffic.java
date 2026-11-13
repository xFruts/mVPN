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
    XuiClientDto.TrafficInfo traffic = client.traffic();
    if (traffic == null) {
      return new XuiClientTraffic(clientEmail, 0L, 0L, 0L, expiry);
    }
    long up = traffic.up() != null ? traffic.up() : 0L;
    long down = traffic.down() != null ? traffic.down() : 0L;
    return new XuiClientTraffic(clientEmail, up, down, up + down, expiry);
  }
}
