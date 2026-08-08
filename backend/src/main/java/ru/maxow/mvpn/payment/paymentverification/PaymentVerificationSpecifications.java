package ru.maxow.mvpn.payment.paymentverification;

import org.springframework.data.jpa.domain.Specification;
import ru.maxow.mvpn.model.VerificationStatus;

import java.time.Instant;

public final class PaymentVerificationSpecifications {

  private PaymentVerificationSpecifications() {}

  public static Specification<PaymentVerification> hasStatus(VerificationStatus status) {
    return (root, query, cb) -> status == null
        ? cb.conjunction()
        : cb.equal(root.get("status"), status);
  }

  public static Specification<PaymentVerification> fullNameContains(String fullName) {
    return (root, query, cb) -> {
      if (fullName == null || fullName.isBlank()) {
        return cb.conjunction();
      }

      String pattern = "%" + escapeLike(fullName.trim().toLowerCase()) + "%";
      return cb.or(
          cb.like(cb.lower(root.get("user").get("fullName")), pattern, '\\'),
          cb.like(cb.lower(root.get("payerFullName")), pattern, '\\')
      );
    };
  }

  public static Specification<PaymentVerification> createdAtFrom(Instant from) {
    return (root, query, cb) -> from == null
        ? cb.conjunction()
        : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
  }

  public static Specification<PaymentVerification> createdAtTo(Instant to) {
    return (root, query, cb) -> to == null
        ? cb.conjunction()
        : cb.lessThanOrEqualTo(root.get("createdAt"), to);
  }

  static String escapeLike(String value) {
    return value
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }
}
