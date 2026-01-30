package ru.maxow.mvpn.payment.paymentverification.dto;

public record CreateUpdatePaymentVerificationDto(
    String billingMonth,
    Long userId,
    String payerFullName,
    String paidAmount,
    String userComment,
    String adminComment
) {
}
