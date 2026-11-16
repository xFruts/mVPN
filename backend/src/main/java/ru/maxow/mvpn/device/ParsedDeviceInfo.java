package ru.maxow.mvpn.device;

/**
 * Распознанная информация об устройстве и клиенте из заголовка User-Agent.
 */
public record ParsedDeviceInfo(
    String clientApp,
    String osName,
    String deviceModel,
    String rawUserAgent
) {}
