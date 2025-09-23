package ru.maxow.mvpn.payment;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing PaymentSettings entities.
 */
public interface PaymentSettingsRepository extends JpaRepository<PaymentSettings, Long> {
}