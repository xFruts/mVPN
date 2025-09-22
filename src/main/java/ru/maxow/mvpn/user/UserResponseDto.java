package ru.maxow.mvpn.user;


public record UserResponseDto(
    Long id,
    String fullName,
    String verificationKey,
    String role
) {
}
