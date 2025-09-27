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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * User entity representing a user in the system.
 */
@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  Long userTelegramId;

  @Column(nullable = false)
  String fullName;

  UUID verificationCode = UUID.randomUUID();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  UserRole role =  UserRole.REGULAR;

  String configFilePath;
}
