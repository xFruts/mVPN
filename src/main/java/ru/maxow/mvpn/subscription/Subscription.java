package ru.maxow.mvpn.subscription;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.vpnconfig.VpnConfig;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Subscription {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  User user;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "config_id", referencedColumnName = "id")
  VpnConfig vpnConfig;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  SubscriptionType type;

  @Column(nullable = false)
  LocalDateTime startDate;

  @Column(nullable = false)
  LocalDateTime endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  SubscriptionStatus status = SubscriptionStatus.ACTIVE;
}
