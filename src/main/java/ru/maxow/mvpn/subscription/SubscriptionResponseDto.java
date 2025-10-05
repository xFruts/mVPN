package ru.maxow.mvpn.subscription;

import java.time.LocalDateTime;
import java.util.List;
import ru.maxow.mvpn.vpnconfig.VpnConfigResponseDto;


public record SubscriptionResponseDto (
    Long id,
    Long userId,
    SubscriptionType type,
    LocalDateTime startDate,
    LocalDateTime endDate,
    SubscriptionStatus status,
    List<VpnConfigResponseDto> vpnConfigs // List of configurations
) {
}
