package ru.maxow.mvpn.user;


import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.UUID;
import ru.maxow.mvpn.util.xss.XssStringJsonSerializer;

/**
 * DTO for user response.
 *
 * @param id               the user ID
 * @param fullName         the full name of the user
 * @param verificationCode the verification code of the user
 * @param role             the role of the user
 * @param userTelegramId   the Telegram ID of the user
 */
public record UserResponseDto(
    Long id,
    @JsonSerialize(using = XssStringJsonSerializer.class)
    String fullName,
    UUID verificationCode,
    String role,
    Long userTelegramId,
    String configFilePath
) {
}
