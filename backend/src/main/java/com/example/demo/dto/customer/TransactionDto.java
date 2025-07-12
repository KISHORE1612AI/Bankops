package com.example.demo.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for transferring transaction data.
 * Use this for both admin/auditor and customer transaction views.
 */
public class TransactionDto {
    private Long id;                   // Transaction ID
    private Long customerId;           // Customer who made the transaction
    private Long recipientId;          // Recipient customer (for transfers, nullable)
    private LocalDateTime timestamp;   // Date and time of transaction
    private String description;        // Description of the transaction
    private BigDecimal amount;         // Amount (currency)
    private String type;               // TRANSFER_OUT, TRANSFER_IN, DEPOSIT, WITHDRAW, etc.
    private String status;             // SUCCESS, FAILED, PENDING, etc.

    public TransactionDto() {}

    public TransactionDto(
            Long id,
            Long customerId,
            Long recipientId,
            LocalDateTime timestamp,
            String description,
            BigDecimal amount,
            String type,
            String status
    ) {
        this.id = id;
        this.customerId = customerId;
        this.recipientId = recipientId;
        this.timestamp = timestamp;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Optional: convenient static factory from Transaction entity
    public static TransactionDto fromEntity(com.example.demo.model.customer.Transaction tx) {
        return new TransactionDto(
                tx.getId(),
                tx.getCustomerId(),
                tx.getRecipientId(),
                tx.getTimestamp(),
                tx.getDescription(),
                tx.getAmount(),
                tx.getType(),
                tx.getStatus()
        );
    }
}