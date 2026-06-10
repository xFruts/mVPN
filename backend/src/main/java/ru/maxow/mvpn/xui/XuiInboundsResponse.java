package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.JsonNode;

import java.util.List;

/**
 * Ответ {@code GET /panel/api/inbounds/list}.
 * Содержит список inbound'ов, включая {@code settings}, {@code streamSettings} как сырой JSON.
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
  }
}
