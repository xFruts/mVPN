package ru.maxow.mvpn.payment.paymentsettings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface PaymentSettingsRepository extends JpaRepository<PaymentSettings, Long> {
  Optional<PaymentSettings> findByBillingMonth(String billingMonth);
}