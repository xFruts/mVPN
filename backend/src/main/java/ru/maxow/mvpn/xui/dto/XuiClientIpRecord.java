package ru.maxow.mvpn.xui.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Запись об IP-адресе подключенного клиента из 3X-UI.
 *
 * @param ip        IP-адрес клиента
 * @param timestamp время последней активности (epoch seconds/millis, может быть null)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record XuiClientIpRecord(
    String ip,
    Long timestamp
) {}
