package ru.maxow.mvpn.vpnconfig;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@DiscriminatorValue("AMNEZIA_WG")
public class AmneziaWgConfig extends VpnConfig {
  String filePath;
}
