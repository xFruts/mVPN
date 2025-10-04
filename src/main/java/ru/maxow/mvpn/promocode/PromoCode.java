package ru.maxow.mvpn.promocode;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.ColumnDefault;
import ru.maxow.mvpn.user.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromoCode {
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
}
