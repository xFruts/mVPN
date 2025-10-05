package ru.maxow.mvpn.vpnconfig;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@DiscriminatorValue("XRAY")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class XrayConfig extends VpnConfig {
  String host;
  Integer port;
  String webBasePath;

  String connectionLink;
}
