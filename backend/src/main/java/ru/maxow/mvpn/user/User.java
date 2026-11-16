package ru.maxow.mvpn.user;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.subscription.Subscription;

/**
 * Сущность пользователя системы.
 */
@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "users")
@FieldDefaults(level = AccessLevel.PRIVATE)
@SQLDelete(sql = "UPDATE users SET is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  Long userTelegramId;

  @Column(name = "telegram_username")
  String telegramUsername;

  public void setTelegramUsername(String username) {
    this.telegramUsername = normalizeTelegramUsername(username);
  }

  /**
   * Нормализует Telegram username, очищая @ и URL-префиксы, и приводя к нижнему регистру.
   *
   * @param username исходный username пользователя
   * @return очищенный и приведенный к нижнему регистру username или null
   */
  public static String normalizeTelegramUsername(String username) {
    if (username == null || username.isBlank()) {
      return null;
    }
    String clean = username.trim();
    if (clean.startsWith("https://t.me/")) {
      clean = clean.substring("https://t.me/".length());
    } else if (clean.startsWith("http://t.me/")) {
      clean = clean.substring("http://t.me/".length());
    } else if (clean.startsWith("t.me/")) {
      clean = clean.substring("t.me/".length());
    }
    if (clean.startsWith("@")) {
      clean = clean.substring(1);
    }
    int slashIdx = clean.indexOf('/');
    if (slashIdx >= 0) {
      clean = clean.substring(0, slashIdx);
    }
    clean = clean.trim();
    return clean.isBlank() ? null : clean.toLowerCase();
  }

  @Column(nullable = false, unique = true)
  String fullName;

  UUID verificationCode = UUID.randomUUID();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  UserRole role = UserRole.REGULAR;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  List<Subscription> subscriptions = new ArrayList<>();

  // Данные для панели 3X-UI
  UUID xuiId = UUID.randomUUID();

  UUID xuiSubscription = UUID.randomUUID();

  @Column(name = "is_deleted", nullable = false)
  boolean isDeleted = false;
}
