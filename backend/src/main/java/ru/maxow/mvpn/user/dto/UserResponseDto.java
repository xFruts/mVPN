package ru.maxow.mvpn.user.dto;


import ru.maxow.mvpn.subscription.dto.SubscriptionResponseDto;

/**
 * DTO for user response.
 *
 * @param id               the user ID
 * @param fullName         the full name of the user
 * @param role             the role of the user
 * @param userTelegramId   the Telegram ID of the user
 */
public record UserResponseDto(
    Long id,
    String fullName,
    Long userTelegramId,
    String role,
    SubscriptionResponseDto subscription
) {
}
