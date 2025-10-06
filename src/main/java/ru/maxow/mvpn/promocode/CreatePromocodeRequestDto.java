package ru.maxow.mvpn.promocode;

import jakarta.validation.constraints.Min;
import org.jetbrains.annotations.NotNull;

public record CreatePromocodeRequestDto(
    @NotNull
    @Min(1)
    Integer usageLimit,
    @NotNull
    @Min(1)
    Integer validDays
) {}
