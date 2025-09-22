package ru.maxow.mvpn.user;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import ru.maxow.mvpn.util.xss.XssStringJsonDeserializer;

public record UserRequestDto(
    @JsonDeserialize(using = XssStringJsonDeserializer.class)
    String fullName,
    Long userTelegramId
) { }
