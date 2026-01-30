package ru.maxow.mvpn.subscription.dto;

import java.time.LocalDateTime;

public record CreateUpdateSubscriptionDto(
    Long userId,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String status
) {
}
