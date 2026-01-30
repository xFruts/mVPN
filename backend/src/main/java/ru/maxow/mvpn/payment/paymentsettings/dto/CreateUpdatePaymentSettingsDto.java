package ru.maxow.mvpn.payment.paymentsettings.dto;

import java.math.BigDecimal;

public record CreateUpdatePaymentSettingsDto(
    String billingMonth,
    BigDecimal expectedAmount,
    String bankName,
    String requisites
) {}
