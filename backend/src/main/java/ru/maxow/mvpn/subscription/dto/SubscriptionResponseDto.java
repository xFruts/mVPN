package ru.maxow.mvpn.subscription.dto;

import java.time.LocalDateTime;

public record SubscriptionResponseDto (
    Long id,
    Long userId,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String status
) {
}
