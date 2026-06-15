package ru.maxow.mvpn.user;

import jakarta.persistence.criteria.Subquery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import ru.maxow.mvpn.model.UserRole;
import ru.maxow.mvpn.subscription.Subscription;

@Slf4j
public final class UserSpecifications {

  private UserSpecifications() {}

  public static Specification<User> hasRole(String roleStr) {
    return (root, query, cb) -> {
      if (roleStr == null || roleStr.isBlank()) return cb.conjunction();

      try {
        UserRole roleEnum = UserRole.valueOf(roleStr.toUpperCase());
        return cb.equal(root.get("role"), roleEnum);
      } catch (IllegalArgumentException e) {
        return cb.disjunction();
      }
    };
  }

  public static Specification<User> hasSubscriptionStatus(String status) {
    return (root, query, cb) -> {
      if (status == null || status.isBlank()) return cb.conjunction();

      Subquery<Long> subquery = query.subquery(Long.class);
      var sub = subquery.from(Subscription.class);
      subquery.select(sub.get("id"))
          .where(
              cb.equal(sub.get("user"), root),
              cb.equal(sub.get("status"), status)
          );
      return cb.exists(subquery);
    };
  }

  public static Specification<User> nameContains(String search) {
    return (root, query, cb) -> {
      if (search == null || search.isBlank()) return cb.conjunction();

      String pattern = "%" + search.toLowerCase() + "%";
      return cb.like(cb.lower(root.get("fullName")), pattern);
    };
  }
}
