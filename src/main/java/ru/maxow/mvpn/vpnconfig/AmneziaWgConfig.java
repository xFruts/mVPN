package ru.maxow.mvpn.vpnconfig;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "amnezia_wg_configs")
@Getter
@Setter
@DiscriminatorValue("AMNEZIA_WG")
public class AmneziaWgConfig extends VpnConfig{
  String filePath;
}
