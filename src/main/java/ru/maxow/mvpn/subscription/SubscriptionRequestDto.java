package ru.maxow.mvpn.subscription;

import org.jetbrains.annotations.NotNull;

public record SubscriptionRequestDto (
    @NotNull
    SubscriptionType type
) {
}
