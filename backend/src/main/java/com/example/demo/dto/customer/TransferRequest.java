package com.example.demo.dto.customer;

import java.math.BigDecimal;

public class TransferRequest {
    private String recipientAccountNumber;
    private BigDecimal amount;

    // Getters and setters
    public String getRecipientAccountNumber() {
        return recipientAccountNumber;
    }
    public void setRecipientAccountNumber(String recipientAccountNumber) {
        this.recipientAccountNumber = recipientAccountNumber;
    }
    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}