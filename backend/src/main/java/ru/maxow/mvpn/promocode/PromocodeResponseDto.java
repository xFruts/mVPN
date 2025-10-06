package ru.maxow.mvpn.promocode;

import java.time.LocalDateTime;

public record PromocodeResponseDto(
    Long id,
    String code,
    Integer usage,
    Integer usageLimit,
    PromocodeStatus status,
    LocalDateTime expirationDate
) {}
