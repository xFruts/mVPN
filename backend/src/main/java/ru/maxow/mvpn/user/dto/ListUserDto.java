package ru.maxow.mvpn.user.dto;

import java.time.LocalDateTime;
import java.util.List;
import ru.maxow.mvpn.subscription.Protocol;
import ru.maxow.mvpn.subscription.SubscriptionStatus;
import ru.maxow.mvpn.subscription.SubscriptionType;

public record ListUserDto (
    Long id,
    String fullName,
    String role,
    SubscriptionType subscriptionType,
    SubscriptionStatus subscriptionStatus,
    List<Protocol> protocols,
    LocalDateTime endDate
) {
}
