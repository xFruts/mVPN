package ru.maxow.mvpn.xui.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Типизированная модель клиента 3X-UI Panel API (v3).
 * Маппится из {@code GET /panel/api/clients/get/{email}} и {@code GET /panel/api/clients/list}.
 * При создании/обновлении серверные поля (uuid, password, auth) генерируются сервером, если не переданы.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record XuiClientDto(
    @JsonProperty("id")
    String uuid,
    String email,
    String subId,
    Long totalGB,
    Long expiryTime,
    Boolean enable,
    @JsonProperty("tgId")
    Long telegramId,
    Integer limitIp,
    String flow,
    String password,
    String security,
    String comment,
    @JsonProperty("inboundIds")
    List<Integer> inboundIds,
    TrafficInfo traffic
) {

  public XuiClientDto {
    inboundIds = inboundIds == null ? null : List.copyOf(inboundIds);
  }

  @Override
  public List<Integer> inboundIds() {
    return inboundIds == null ? null : List.copyOf(inboundIds);
  }

  /**
   * Вложенная структура трафика, приходящая из {@code /panel/api/clients/get/{email}}.
   */
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record TrafficInfo(
      Long up,
      Long down,
      Boolean enable
  ) {}
}
