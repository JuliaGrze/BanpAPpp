package com.bank.bank.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WebhookDTO {
    Long transactionId;
    String email;
    java.math.BigDecimal amount;
    String status;
    String orderId;

//    // getters i setters
//    public Long getTransactionId() { return transactionId; }
//    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
//    public String getEmail() { return email; }
//    public void setEmail(String email) { this.email = email; }
//    public BigDecimal getAmount() { return amount; }
//    public void setAmount(BigDecimal amount) { this.amount = amount; }
//    public String getStatus() { return status; }
//    public void setStatus(String status) { this.status = status; }
//    public LocalDateTime getTimestamp() { return timestamp; }
//    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
