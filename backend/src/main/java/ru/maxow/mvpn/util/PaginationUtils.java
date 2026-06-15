package ru.maxow.mvpn.util;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PaginationUtils {

  private PaginationUtils() {}

  private static final Map<String, String> JPA_PATHS = Map.of(
      "subEndDate", "subscriptions.endDate",
      "subStatus", "subscriptions.status",
      "tariffName", "subscriptions.tariff.name",
      "activeUsers", "usage"
  );

  public static Sort parseSorting(List<String> sort) {
    if (sort == null || sort.isEmpty()) {
      return Sort.unsorted();
    }

    List<Sort> sorts = new ArrayList<>();

    for (int i = 0; i < sort.size(); i++) {
      String part = sort.get(i);

      if (part.contains(",")) {
        String[] split = part.split(",");
        Sort.Direction dir = split.length > 1 && split[1].equalsIgnoreCase("desc")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
        sorts.add(createSort(dir, split[0]));
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
        sorts.add(createSort(direction, part));
      }
    }

    return sorts.stream()
        .reduce(Sort.unsorted(), Sort::and);
  }

  private static Sort createSort(Sort.Direction direction, String field) {
    String path = JPA_PATHS.get(field);
    return path != null
        ? JpaSort.unsafe(direction, path)
        : Sort.by(direction, field);
  }
}