package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Данные о трафике клиента, получаемые из 3X-UI.
 * В новом API трафик приходит как часть {@code GET /panel/api/clients/get/{email}},
 * но также может быть построен вручную при агрегации.
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

  /**
   * Создаёт «нулевой» трафик для случаев, когда данные недоступны.
   */
  public static XuiClientTraffic zero(String email) {
    return new XuiClientTraffic(email, 0L, 0L, 0L, 0L);
  }
}
