package ru.maxow.mvpn.subscription;

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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.model.SubscriptionStatus;
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
}
