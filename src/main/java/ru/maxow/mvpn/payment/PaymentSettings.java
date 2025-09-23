package ru.maxow.mvpn.payment;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;


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