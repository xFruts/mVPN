package ru.maxow.mvpn.server;

import org.springframework.data.jpa.domain.Specification;
import ru.maxow.mvpn.model.ServerStatus;

public final class ServerSpecifications {

  private ServerSpecifications() {}

  public static Specification<Server> hasStatus(String status) {
    return (root, query, cb) -> {
      if (status == null || status.isBlank()) {
        return cb.conjunction();
      }
      try {
        ServerStatus statusEnum = ServerStatus.valueOf(status.toUpperCase());
        return cb.equal(root.get("status"), statusEnum);
      } catch (IllegalArgumentException e) {
        return cb.disjunction();
      }
    };
  }

  public static Specification<Server> nameOrIpContains(String search) {
    return (root, query, cb) -> {
      if (search == null || search.isBlank()) {
        return cb.conjunction();
      }
      String pattern = "%" + search.toLowerCase() + "%";
      return cb.or(
          cb.like(cb.lower(root.get("name")), pattern),
          cb.like(cb.lower(root.get("ip")), pattern)
      );
    };
  }
}
