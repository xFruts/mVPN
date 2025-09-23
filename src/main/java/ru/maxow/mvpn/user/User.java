package ru.maxow.mvpn.user;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy= GenerationType.IDENTITY)
  private Long id;

  private Long userTelegramId;

  @Column(nullable = false)
  private String fullName;

  private UUID verificationCode = UUID.randomUUID();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role =  UserRole.REGULAR;
}
