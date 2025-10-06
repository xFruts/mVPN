package ru.maxow.mvpn.promocode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;

/**
 * Entity representing a promotional code.
 */
@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Promocode {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false, unique = true)
  String code;

  @Column(nullable = false)
  LocalDateTime expirationDate;

  @Column(nullable = false)
  @ColumnDefault("1")
  Integer usageLimit = 1;

  @Column(nullable = false)
  @ColumnDefault("0")
  Integer usage = 0;

  @Column(nullable = false)
  PromocodeStatus status;
}
