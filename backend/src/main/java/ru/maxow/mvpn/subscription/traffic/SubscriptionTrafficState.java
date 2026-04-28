package ru.maxow.mvpn.subscription.traffic;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.xui.TrafficSyncSource;

import java.time.Instant;

@Entity
@Table(name = "subscription_traffic_state")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionTrafficState {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false, unique = true)
  Long subscriptionId;

  @Column(nullable = false)
  Long usedBytes;

  @Column(nullable = false)
  Long usedUploadBytes;

  @Column(nullable = false)
  Long usedDownloadBytes;

  @Column(nullable = false)
  Instant lastSyncedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  TrafficSyncSource source;
}
