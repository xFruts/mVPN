package ru.maxow.mvpn.subscription;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.user.User;
import ru.maxow.mvpn.vpnconfig.VpnConfig;

/**
 * Entity representing a subscription.
 */
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

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "subscription")
  List<VpnConfig> vpnConfigs = new ArrayList<>();

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
