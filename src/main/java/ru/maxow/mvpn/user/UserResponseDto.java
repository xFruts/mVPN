package ru.maxow.mvpn.user;


import java.util.UUID;

public record UserResponseDto(
    Long id,
    String fullName,
    UUID verificationCode,
    String role,
    Long userTelegramId
) {
}
