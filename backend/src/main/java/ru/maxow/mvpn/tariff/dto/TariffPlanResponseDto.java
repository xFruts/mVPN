package ru.maxow.mvpn.tariff.dto;

import org.springframework.data.util.Pair;
import java.util.List;

public record TariffPlanResponseDto(
    Long id,
    String name,
    Integer maxDevices,
    Integer trafficLimitGb,
    List<Pair<Long, String>> serverLocation
) {
}
