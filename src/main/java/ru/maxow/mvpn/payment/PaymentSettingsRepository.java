package ru.maxow.mvpn.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

public interface PaymentSettingsRepository extends JpaRepository<PaymentSettings, Long> {
}