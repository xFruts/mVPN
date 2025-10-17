package ru.maxow.mvpn.server.dto;

import ru.maxow.mvpn.server.ServerStatus;

public record CreateUpdateServerRequestDto (
    String name,
    String location,
    String ip,
    ServerStatus status,
    Integer maxUsers,
    Integer maxTraffic, // in GB
    String login,
    String password,

    //For 3x-ui panel
    String xuiLogin,
    String xuiPassword,
    Integer port,
    String webBasePath
) {}
