package ru.maxow.mvpn.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ru.maxow.mvpn.user.UserRole;

public record CreateUserRequestDto (
    @NotBlank
    String fullName,
    Long userTelegramId,
    @NotNull
    UserRole role
) {}
