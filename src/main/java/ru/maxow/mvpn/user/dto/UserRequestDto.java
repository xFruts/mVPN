package ru.maxow.mvpn.user.dto;

/**
 * Data Transfer Object for user requests.
 *
 * @param fullName       the full name of the user
 * @param userTelegramId the Telegram ID of the user
 */
public record UserRequestDto(
    String fullName,
    Long userTelegramId
) {}
