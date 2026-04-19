package ru.maxow.mvpn.payment.paymentverification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentVerificationRepository extends JpaRepository<PaymentVerification, Long>,
	JpaSpecificationExecutor<PaymentVerification> {
}
