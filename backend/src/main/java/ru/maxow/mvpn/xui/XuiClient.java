package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class XuiClient {
  private String id;
  private String email;
  private String flow;
  private String comment;
  private Integer reset;
}
