package ru.maxow.mvpn.user.dto;

import java.time.LocalDateTime;
import ru.maxow.mvpn.subscription.SubscriptionStatus;

public record ListUserDto (
    Long id,
    String fullName,
    String role,
    SubscriptionStatus subscriptionStatus,
    LocalDateTime endDate
) {
}
