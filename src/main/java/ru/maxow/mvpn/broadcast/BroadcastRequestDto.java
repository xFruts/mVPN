package ru.maxow.mvpn.broadcast;

import java.util.List;

/**
 * DTO for broadcast request.
 *
 * @param message       the message to be broadcasted
 * @param targetAudience the target audience for the broadcast
 * @param customUserIds list of custom user IDs to receive the message (if applicable)
 */
public record BroadcastRequestDto(
    String message,
    TargetAudience targetAudience,
    List<Long> customUserIds
) {
}
