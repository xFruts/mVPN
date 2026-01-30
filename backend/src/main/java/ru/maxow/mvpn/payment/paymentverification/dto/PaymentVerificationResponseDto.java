package ru.maxow.mvpn.payment.paymentverification.dto;

import java.math.BigDecimal;

public record PaymentVerificationResponseDto (
    Long id,
    Long userId,
    Integer billingMonth,
    String payerFullName,
    BigDecimal paidAmount,
    String status,
    String userComment,
    String adminComment
) {}
