package ru.maxow.mvpn.tariff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.server.Server;

@Entity
@Table(name = "tariff_plan")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Tariff {

  private static final long BYTES_IN_GIGABYTE = 1024L * 1024L * 1024L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false, unique = true)
  String name;

  @Column(nullable = false)
  Integer maxDevices;

  @Column(nullable = false)
  Integer trafficLimitGb;

  @Column(nullable = false)
  Integer durationOfDays;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "tariff_plan_servers",
      joinColumns = @JoinColumn(name = "tariff_id"),
      inverseJoinColumns = @JoinColumn(name = "server_id")
  )
  Set<Server> servers = new HashSet<>();

  public long getTrafficLimitBytes() {
    return trafficLimitGb != null ? trafficLimitGb * BYTES_IN_GIGABYTE : 0L;
  }
}
