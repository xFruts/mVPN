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

/**
 * Состояние суммарного использованного трафика подписки за текущий период.
 */
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

  /**
   * Проверяет, превышен ли заданный лимит байт.
   *
   * @param limitBytes лимит трафика в байтах
   * @return true, если фактически использованный объем больше либо равен лимиту
   */
  public boolean isLimitExceeded(long limitBytes) {
    if (limitBytes <= 0) {
      return false;
    }
    return getEffectiveUsedBytes() >= limitBytes;
  }

  /**
   * Сбрасывает счетчики использованного трафика для нового расчетного периода.
   */
  public void resetPeriod(Instant now) {
    this.usedBytes = 0L;
    this.usedUploadBytes = 0L;
    this.usedDownloadBytes = 0L;
    this.periodStartTotalBytes = 0L;
    this.periodStartUploadBytes = 0L;
    this.periodStartDownloadBytes = 0L;
    this.periodStartAt = now;
  }

  /**
   * Фабричный метод для создания начального состояния трафика подписки.
   */
  public static SubscriptionTrafficState initial(Long subscriptionId, Instant now) {
    SubscriptionTrafficState state = new SubscriptionTrafficState();
    state.setSubscriptionId(subscriptionId);
    state.resetPeriod(now);
    state.setLastSyncedAt(now);
    state.setSource(TrafficSyncSource.XUI_SYNC);
    return state;
  }
}
