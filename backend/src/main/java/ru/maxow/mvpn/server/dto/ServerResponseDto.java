package ru.maxow.mvpn.server.dto;

import ru.maxow.mvpn.server.ServerStatus;

public record ServerResponseDto(
    Long id,
    String name,
    String location,
    String ip,
    ServerStatus status,
    Integer load,
    Integer usage,
    Integer maxUsers,
    Integer maxTraffic, // in GB
    String ping,
    Double uptime
) {}
