package com.bank.bank.dto;

import com.bank.bank.model.TransactionStatus;

import java.math.BigDecimal;

public record InitTransactionResponse(
        String transactionId,
        String status,
        String orderId,
        BigDecimal amount
) {}

