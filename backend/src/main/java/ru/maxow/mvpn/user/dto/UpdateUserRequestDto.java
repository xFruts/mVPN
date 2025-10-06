package ru.maxow.mvpn.user.dto;

import java.time.LocalDateTime;
import ru.maxow.mvpn.subscription.SubscriptionStatus;
import ru.maxow.mvpn.subscription.SubscriptionType;
import ru.maxow.mvpn.user.UserRole;

public record UpdateUserRequestDto(
    String fullName,
    Long userTelegramId,
    UserRole role,
    SubscriptionStatus subscriptionStatus,
    SubscriptionType subscriptionType,
    LocalDateTime subscriptionEndDate
) {
}
