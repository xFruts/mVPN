package ru.maxow.mvpn.xui.dto;

import java.util.List;

/**
 * Тело запроса для {@code POST /panel/api/clients/add}.
 * Содержит типизированную модель клиента и список inbound ID, к которым привязать клиента.
 */
public record XuiCreateUpdateClientRequestDto(
    XuiClientDto client,
    List<Integer> inboundIds
) {
}
