package ru.maxow.mvpn.vpnconfig;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import ru.maxow.mvpn.subscription.Subscription;

@Entity
@Table(name = "vpn_configs")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "protocol", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class VpnConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    Subscription subscription;
}
