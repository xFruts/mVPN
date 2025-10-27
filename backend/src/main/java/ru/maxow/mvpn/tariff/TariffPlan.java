package ru.maxow.mvpn.tariff;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.server.Server;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TariffPlan {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false, unique = true)
  String name;

  @Column(nullable = false)
  Integer maxDevices;

  @Column(nullable = false)
  Integer trafficLimitGb;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "tariff_plan_servers",
      joinColumns = @JoinColumn(name = "tariff_plan_id"),
      inverseJoinColumns = @JoinColumn(name = "server_id")
  )
  Set<Server> servers = new HashSet<>();
}
