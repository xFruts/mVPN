package ru.maxow.mvpn.payment;

import java.time.LocalDate;

public record PaymentSettingsRequestDto(
    Long phoneNumber,
    String bankName,
    Double price,
    LocalDate paymentDate
) {
}
