package ru.maxow.mvpn.server;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Convert;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.model.ServerStatus;
import ru.maxow.mvpn.util.converter.AttributeEncryptor;

@Entity
@Table(name = "servers")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Server {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  // For admin panel
  String name;

  String location;

  String ip;

  ServerStatus status;

  Integer load;

  Integer usage;

  Integer maxUsers;

  Integer maxTraffic; // in GB

  String ping;

  Double uptime;

  String countryEmoji;

  //For connect to server and 3x-ui
  @Convert(converter = AttributeEncryptor.class)
  String login;

  @Convert(converter = AttributeEncryptor.class)
  String password;

  @Convert(converter = AttributeEncryptor.class)
  String xuiLogin;

  @Convert(converter = AttributeEncryptor.class)
  String xuiPassword;

  Integer port;
  String webBasePath;

  //For uptime calculation
  Long successfulChecks = 0L;
  Long failedChecks = 0L;

  @Column(name = "host_key", columnDefinition = "TEXT")
  private String hostKey;
}
