package ru.maxow.mvpn.subscription.traffic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Хранит последнее зафиксированное состояние счётчиков трафика сервера для подписки.
 * Используется для вычисления дельт трафика и устойчивости к сбросам нод 3X-UI.
 */
@Entity
@Table(
    name = "subscription_server_traffic",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sub_server_traffic",
        columnNames = {"subscription_id", "server_id"}
    )
)
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubscriptionServerTraffic {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "subscription_id", nullable = false)
  Long subscriptionId;

  @Column(name = "server_id", nullable = false)
  Long serverId;

  @Column(name = "last_upload_bytes", nullable = false)
  Long lastUploadBytes = 0L;

  @Column(name = "last_download_bytes", nullable = false)
  Long lastDownloadBytes = 0L;

  @Column(name = "last_synced_at", nullable = false)
  Instant lastSyncedAt;
}
