package ru.maxow.mvpn.subscription.traffic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.xui.dto.TrafficSyncSource;

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

  @Column
  Long periodStartTotalBytes;

  @Column
  Long periodStartUploadBytes;

  @Column
  Long periodStartDownloadBytes;

  @Column
  Instant periodStartAt;

  @Column(nullable = false)
  Instant lastSyncedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  TrafficSyncSource source;

  public long getEffectiveUsedBytes() {
    return usedBytes != null ? usedBytes : 0L;
  }

  public long getEffectiveUploadBytes() {
    return usedUploadBytes != null ? usedUploadBytes : 0L;
  }

  public long getEffectiveDownloadBytes() {
    return usedDownloadBytes != null ? usedDownloadBytes : 0L;
  }

  public boolean isLimitExceeded(long limitBytes) {
    if (limitBytes <= 0) {
      return false;
    }
    return getEffectiveUsedBytes() >= limitBytes;
  }
}
