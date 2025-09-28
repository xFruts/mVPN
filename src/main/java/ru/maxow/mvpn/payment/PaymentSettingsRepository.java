package ru.maxow.mvpn.payment;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;


/**
 * Repository interface for managing PaymentSettings entities.
 */
public interface PaymentSettingsRepository extends JpaRepository<PaymentSettings, Long> {
  /** Finds the most recently created PaymentSettings entity. */
  Optional<PaymentSettings> findTopByOrderByIdDesc();
}