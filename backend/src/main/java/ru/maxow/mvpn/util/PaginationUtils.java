package ru.maxow.mvpn.util;

import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class PaginationUtils {

  private PaginationUtils() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static Sort parseSorting(List<String> sort) {
    if (sort == null || sort.isEmpty()) {
      return Sort.unsorted();
    }

    List<Sort.Order> orders = new ArrayList<>();

    for (int i = 0; i < sort.size(); i++) {
      String part = sort.get(i);

      if (part.contains(",")) {
        String[] split = part.split(",");
        Sort.Direction dir = split.length > 1 && split[1].equalsIgnoreCase("desc")
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;
        orders.add(new Sort.Order(dir, split[0]));
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
        orders.add(new Sort.Order(direction, part));
      }
    }
    return Sort.by(orders);
  }
}