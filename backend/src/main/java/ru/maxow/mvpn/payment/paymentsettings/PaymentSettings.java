package ru.maxow.mvpn.payment.paymentsettings;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(
    name = "payment_settings",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"billingMonth", "createdBy"})
    }
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentSettings {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(name = "billing_month", nullable = false, length = 7)
  String billingMonth;

  @Column(name = "expected_amount", nullable = false, precision = 19, scale = 2)
  BigDecimal expectedAmount; // RUB

  @Column(name = "bank_name", nullable = false, length = 100)
  String bankName;

  @Column(name = "requisites", nullable = false, length = 500)
  String requisites;

  // TODO: Пока что данное поле игнорируется. После привязки к mID нужно будет связать с пользователем.
  @Column(name = "created_by")
  Long createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  Instant createdAt;

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}