package ru.maxow.mvpn.promocode;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreatePromocodeRequestDto(
    @NotNull
    @Min(1)
    Integer usageLimit,
    @NotNull
    @Min(1)
    Integer validDays
) {}
