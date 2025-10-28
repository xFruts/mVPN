package ru.maxow.mvpn.tariff.dto;

import java.util.List;

public record CreateUpdateRequestTariffPlanDto(
    String name,
    Integer maxDevices,
    Integer trafficLimitGb,
    List<Long> serverIds
) {
}
