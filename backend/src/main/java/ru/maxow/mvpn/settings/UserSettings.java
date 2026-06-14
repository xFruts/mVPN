package ru.maxow.mvpn.settings;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Table(name = "users_settings")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserSettings {
  @Id
  String keycloakUserId;

  @Column(name = "theme", nullable = false)
  String theme;
}
