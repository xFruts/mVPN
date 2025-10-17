package ru.maxow.mvpn.xui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record XuiLoginResponse (
    boolean success,
    String msg
) {
}
