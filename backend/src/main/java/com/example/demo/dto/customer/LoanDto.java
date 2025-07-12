package com.example.demo.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for loan summary/status, includes basic customer info for dashboard.
 */
public class LoanDto {
    private Long id;
    private Long customerId;
    private String customerName; // NEW
    private String customerEmail; // NEW (optional)
    private String loanType;
    private BigDecimal amountRequested;
    private String status;
    private LocalDateTime appliedAt;
    private String reviewedBy;
    private LocalDateTime reviewedAt;

    public LoanDto() {}

    // Updated constructor
    public LoanDto(Long id, Long customerId, String customerName, String customerEmail,
                   String loanType, BigDecimal amountRequested, String status,
                   LocalDateTime appliedAt, String reviewedBy, LocalDateTime reviewedAt) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.loanType = loanType;
        this.amountRequested = amountRequested;
        this.status = status;
        this.appliedAt = appliedAt;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
    }

    // Getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }

    public BigDecimal getAmountRequested() { return amountRequested; }
    public void setAmountRequested(BigDecimal amountRequested) { this.amountRequested = amountRequested; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public String getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
}