package ru.maxow.mvpn.payment.paymentverification;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.model.VerificationStatus;
import ru.maxow.mvpn.user.User;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(
    name = "payment_verifications",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"billing_month", "user_id"})
    },
    indexes = {
        @Index(columnList = "status"),
        @Index(columnList = "user_id")
    }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentVerification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "billing_month", nullable = false, length = 7)
  String billingMonth; // 2026-01

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  User user;

  @Column(name = "payer_full_name", nullable = false, length = 200)
  String payerFullName;

  @Column(name = "paid_amount", nullable = false, precision = 19, scale = 2)
  BigDecimal paidAmount; // RUB

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  VerificationStatus status;

  @Column(name = "user_comment", length = 1000)
  String userComment;

  @Column(name = "admin_comment", length = 1000)
  String adminComment;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @Column(name = "verified_at")
  Instant verifiedAt;

  @Column(name = "verified_by")
  Long verifiedBy;

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
