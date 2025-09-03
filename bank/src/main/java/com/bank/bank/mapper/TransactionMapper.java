package com.bank.bank.mapper;

import com.bank.bank.dto.TransactionDTO;
import com.bank.bank.model.Transaction;

public class TransactionMapper {
    public static TransactionDTO toDTO(Transaction tx) {
        String email = (tx.getUser() != null) ? tx.getUser().getEmail() : null;
        return new TransactionDTO(
                tx.getId(),
                tx.getAmount(),          // BigDecimal
                tx.getStatus(),
                tx.getCreatedAt(),       // LocalDateTime
                email
        );
    }
}
