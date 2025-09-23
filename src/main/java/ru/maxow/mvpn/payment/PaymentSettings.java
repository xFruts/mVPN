package ru.maxow.mvpn.payment;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Entity representing payment settings.
 */
@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "payments")
public class PaymentSettings {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long phoneNumber;

  private String bankName;

  private Double price;

  private LocalDate paymentDate;
}