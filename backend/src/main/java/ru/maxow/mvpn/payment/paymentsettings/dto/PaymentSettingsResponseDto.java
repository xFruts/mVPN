package ru.maxow.mvpn.payment.paymentsettings.dto;

import java.math.BigDecimal;

public record PaymentSettingsResponseDto (
    Long id,
    String billingMonth,
    String paymentDate,
    BigDecimal expectedAmount,
    String bankName,
    String requisites
)
{}
