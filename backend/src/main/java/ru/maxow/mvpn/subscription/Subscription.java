package ru.maxow.mvpn.subscription;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.model.SubscriptionStatus;
import ru.maxow.mvpn.tariff.Tariff;
import ru.maxow.mvpn.user.User;

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

  @Column(nullable = false)
  OffsetDateTime startDate;

  @Column(nullable = false)
  OffsetDateTime endDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  SubscriptionStatus status = SubscriptionStatus.ACTIVE;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tariff_id", nullable = false)
  Tariff tariff;
}
