package ru.maxow.mvpn.payment.paymentverification;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.maxow.mvpn.model.VerificationStatus;

@Repository
public interface PaymentVerificationRepository extends JpaRepository<PaymentVerification, Long>,
	JpaSpecificationExecutor<PaymentVerification> {

  long countByStatus(VerificationStatus status);

  @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM PaymentVerification p WHERE p.status = :status")
  BigDecimal sumPaidAmountByStatus(@Param("status") VerificationStatus status);
}
