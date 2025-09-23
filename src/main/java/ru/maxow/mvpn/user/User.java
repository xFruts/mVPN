package ru.maxow.mvpn.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * User entity representing a user in the system.
 */
@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private Long userTelegramId;

  @Column(nullable = false)
  private String fullName;

  private UUID verificationCode = UUID.randomUUID();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private UserRole role =  UserRole.REGULAR;
}
