package ru.maxow.mvpn.payment;

import java.time.LocalDate;

/**
 * DTO for payment settings request.
 *
 * @param phoneNumber the phone number associated with the payment
 * @param bankName    the name of the bank
 * @param price       the payment amount
 * @param paymentDate the date of the payment
 */
public record PaymentSettingsRequestDto(
    Long phoneNumber,
    String bankName,
    Double price,
    LocalDate paymentDate
) {
}
