package com.bank.bank.dto;

import java.math.BigDecimal;

public record InitTransactionRequest(
        String orderId,
        BigDecimal amount,
        String currency,
        String callbackUrl,
        String description,
        String customerEmail
) {}