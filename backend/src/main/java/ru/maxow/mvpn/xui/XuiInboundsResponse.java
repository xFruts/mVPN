package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class XuiInboundsResponse {
  private boolean success;
  private String msg;
  private List<Inbound> obj;

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Inbound {
    private int id;
    private int port;
    private String tag;
    private String protocol;
    private String settings; //JSON as String
    private String remark;
    private String streamSettings; //JSON as String
  }
}
