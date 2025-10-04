package ru.maxow.mvpn.subscription;

import java.time.LocalDateTime;

public record SubscriptionResponseDto (
    Long id,
    Long userId,
    Long configId,
    String configType,
    LocalDateTime startDate,
    LocalDateTime endDate,
    SubscriptionStatus status
) {
}
