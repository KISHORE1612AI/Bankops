package com.example.demo.model.customer;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Timestamp is required")
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotNull(message = "Transaction type is required")
    @Size(min = 1, max = 50)
    @Column(nullable = false, length = 50)
    private String type; // e.g. DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN

    @Size(max = 255)
    @Column(length = 255)
    private String description;

    @NotNull(message = "Status is required")
    @Size(min = 1, max = 50)
    @Column(nullable = false, length = 50)
    private String status; // e.g. SUCCESS, FAILED, PENDING

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer; // The customer who owns or initiated the transaction

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private Customer recipient; // Nullable, for transfer transactions only

    // --- Super Admin/Officer fields (optional for auditing) ---
    @Column(name = "created_by_emp_id", length = 50)
    private String createdByEmpId; // For auditing who created/approved the transaction

    // Custom getter for customerId (for DTOs/services)
    public Long getCustomerId() {
        return customer != null ? customer.getId() : null;
    }

    // Custom getter for recipientId (for DTOs/services)
    public Long getRecipientId() {
        return recipient != null ? recipient.getId() : null;
    }

    // Transaction type enum (for reference)
    public enum TransactionType {
        DEPOSIT,
        WITHDRAW,
        TRANSFER_OUT,
        TRANSFER_IN
    }

    // Transaction status enum (for reference)
    public enum TransactionStatus {
        SUCCESS,
        FAILED,
        PENDING
    }

    // Automatically set timestamp if not set
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    // Check if this transaction is a transfer out or transfer in
    public boolean isTransfer() {
        return TransactionType.TRANSFER_OUT.name().equals(this.type) ||
               TransactionType.TRANSFER_IN.name().equals(this.type);
    }

    // Validate transaction fields for business rules
    public boolean isValid() {
        return customer != null
            && amount != null
            && amount.compareTo(BigDecimal.ZERO) > 0
            && type != null
            && status != null
            && (!isTransfer() || recipient != null);
    }
}