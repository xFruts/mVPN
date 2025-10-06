package ru.maxow.mvpn.payment;

/**
 * Data Transfer Object for Payment Settings response.
 *
 * @param phoneNumber the phone number associated with the payment settings
 * @param bankName    the name of the bank
 * @param price       the price associated with the payment settings
 * @param paymentDate the date of the payment
 */
public record PaymentSettingsResponseDto(
    Long phoneNumber,
    String bankName,
    Double price,
    String paymentDate
) {
}
