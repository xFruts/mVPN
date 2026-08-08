package ru.maxow.mvpn.util;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
import ru.maxow.mvpn.util.exception.BadRequestException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PaginationUtils {

  private PaginationUtils() {}

  private static final Map<String, String> JPA_PATHS = Map.ofEntries(
      Map.entry("subEndDate", "subscriptions.endDate"),
      Map.entry("subStatus", "subscriptions.status"),
      Map.entry("tariffName", "subscriptions.tariff.name"),
      Map.entry("activeUsers", "usage"),
      Map.entry("user.fullName", "user.fullName")
  );

  public static Sort parseSorting(List<String> sort) {
    return parseSorting(sort, null, Sort.unsorted());
  }

  public static Sort parseSorting(List<String> sort, Set<String> allowedProperties, Sort defaultSort) {
    if (sort == null || sort.isEmpty()) {
      return defaultSort;
    }

    List<Sort> sorts = new ArrayList<>();

    for (int i = 0; i < sort.size(); i++) {
      String part = sort.get(i);

      if (part.contains(",")) {
        String[] split = part.split(",");
        Sort.Direction dir = split.length > 1 && split[1].equalsIgnoreCase("desc")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
        sorts.add(createSort(dir, split[0], allowedProperties));
      }
      else {
        Sort.Direction direction = Sort.Direction.ASC;

        if (i + 1 < sort.size()) {
          String nextPart = sort.get(i + 1);
          if (nextPart.equalsIgnoreCase("asc")
              || nextPart.equalsIgnoreCase("desc")) {
            direction = Sort.Direction.fromString(nextPart);
            i++;
          }
        }
        sorts.add(createSort(direction, part, allowedProperties));
      }
    }

    return sorts.stream()
        .reduce(Sort.unsorted(), Sort::and);
  }

  private static Sort createSort(Sort.Direction direction, String field, Set<String> allowedProperties) {
    if (allowedProperties != null && !allowedProperties.contains(field)) {
      throw new BadRequestException("Unsupported sort property: " + field);
    }

    String path = JPA_PATHS.get(field);
    return path != null
        ? JpaSort.unsafe(direction, path)
        : Sort.by(direction, field);
  }
}
