package ru.maxow.mvpn.broadcast;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Entity representing a broadcast message to be sent to users.
 */
@Entity
@Table(name = "broadcasts")
@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Broadcast {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  String message;

  TargetAudience targetAudience;

  List<Long> customUserIds;
}