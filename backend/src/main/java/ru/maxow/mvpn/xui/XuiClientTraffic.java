package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class XuiClientTraffic {

  @JsonProperty("id")
  private Long id;

  @JsonProperty("inboundId")
  private Long inboundId;

  @JsonProperty("enable")
  private Boolean enable;

  @JsonProperty("email")
  private String email;

  @JsonProperty("up")
  private Long upload;

  @JsonProperty("down")
  private Long download;

  @JsonProperty("allTime")
  private Long allTime;

  @JsonProperty("total")
  private Long total;

  @JsonProperty("expiryTime")
  private Long expiryTime;

  @JsonProperty("reset")
  private Integer reset;

  @JsonProperty("lastOnline")
  private Long lastOnline;
}


