package ru.maxow.mvpn.promocode;

import org.springframework.data.jpa.domain.Specification;
import ru.maxow.mvpn.model.PromocodeStatus;

public class PromocodeSpecifications {

  private PromocodeSpecifications() {}

  public static Specification<Promocode> hasStatus(String status) {
    return (root, query, cb) -> {
      if (status == null || status.isBlank()) {
        return cb.conjunction();
      }
      try {
        PromocodeStatus statusEnum = PromocodeStatus.valueOf(status.toUpperCase());
        return cb.equal(root.get("status"), statusEnum);
      } catch (IllegalArgumentException e) {
        return cb.disjunction();
      }
    };
  }

  public static Specification<Promocode> codeContains(String search) {
    return (root, query, cb) -> {
      if (search == null || search.isBlank()) {
        return cb.conjunction();
      }
      String pattern = "%" + search.toLowerCase() + "%";
      return cb.or(
          cb.like(cb.lower(root.get("code")), pattern)
      );
    };
  }
}
