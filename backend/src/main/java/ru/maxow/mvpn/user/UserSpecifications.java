package ru.maxow.mvpn.user;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import ru.maxow.mvpn.model.UserRole;

@Slf4j
public final class UserSpecifications {

  private UserSpecifications() {}

  private static final Set<String> SUBSCRIPTION_SORT_FIELDS = Set.of("endDate", "subscriptionStatus");

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

  public static Specification<User> hasTariff(String tariff) {
      return (root, query, cb) -> {
        if (tariff == null || tariff.isBlank()) return cb.conjunction();

        query.distinct(true);

        Join<Object, Object> subscriptionsJoin = root.join("subscriptions",
            JoinType.INNER);
        return cb.equal(subscriptionsJoin.get("tariff").get("name"), tariff);
      };
  }

  public static Specification<User> hasSubscriptionStatus(String status) {
    return (root, query, cb) -> {
      if (status == null || status.isBlank()) return cb.conjunction();

      query.distinct(true);
      Join<Object, Object> subscriptionsJoin = root.join("subscriptions",
          JoinType.INNER);

      return cb.equal(subscriptionsJoin.get("status"), status);
    };
  }

  public static Specification<User> nameContains(String search) {
    return (root, query, cb) -> {
      if (search == null || search.isBlank()) return cb.conjunction();

      String pattern = "%" + search.toLowerCase() + "%";
      return cb.like(cb.lower(root.get("fullName")), pattern);
    };
  }

  public static Specification<User> distinctIfSubscriptionSort(List<String> sort) {
    return (root, query, cb) -> {
      boolean hasSubSort = sort != null && sort.stream()
          .anyMatch(s -> SUBSCRIPTION_SORT_FIELDS.stream()
              .anyMatch(s::startsWith));
      if (hasSubSort) {
        query.distinct(true);
      }
      return cb.conjunction();
    };
  }
}
