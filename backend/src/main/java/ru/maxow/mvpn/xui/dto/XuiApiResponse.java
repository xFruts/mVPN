package ru.maxow.mvpn.xui.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Универсальный response-wrapper 3X-UI Panel API.
 * Все ответы имеют структуру {@code {"success": bool, "msg": "...", "obj": ...}}.
 *
 * @param <T> тип поля {@code obj}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record XuiApiResponse<T>(
    boolean success,
    String msg,
    T obj
) {
}
