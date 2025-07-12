package com.example.demo.model.customer;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY is correct for entity design, but must always use JOIN FETCH or DTO for dashboard queries
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;

    @NotNull(message = "Loan type is required")
    @Size(min = 1, max = 50)
    @Column(name = "loan_type", nullable = false, length = 50)
    private String loanType;

    @NotNull(message = "Amount requested is required")
    @Positive(message = "Amount requested must be positive")
    @Column(name = "amount_requested", nullable = false, precision = 19, scale = 4)
    private BigDecimal amountRequested;

    @NotNull(message = "Status is required")
    @Size(min = 1, max = 50)
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @NotNull(message = "Applied date is required")
    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Size(max = 100)
    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // For Super Admin assignment/reassignment
    @Column(name = "assigned_to_emp_id", length = 50)
    private String assignedToEmpId;

    // Enum definitions (not persisted)
    public enum LoanType {
        GOLD, HOME, EDUCATION, VEHICLE, PERSONAL, BUSINESS
    }

    public enum LoanStatus {
        PENDING, UNDER_REVIEW, APPROVED, REJECTED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = LoanStatus.PENDING.name();
        }
    }

    public boolean isReviewed() {
        return reviewedBy != null && reviewedAt != null;
    }

    public boolean isPending() {
        return LoanStatus.PENDING.name().equals(this.status);
    }

    public boolean isApproved() {
        return LoanStatus.APPROVED.name().equals(this.status);
    }

    public boolean isRejected() {
        return LoanStatus.REJECTED.name().equals(this.status);
    }

    public boolean isValid() {
        return customer != null
                && loanType != null
                && amountRequested != null
                && amountRequested.compareTo(BigDecimal.ZERO) > 0
                && status != null;
    }

    public void review(String reviewer, LoanStatus newStatus) {
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
        this.status = newStatus.name();
    }

    public Long getCustomerId() {
        return customer != null ? customer.getId() : null;
    }

    public void assignToEmployee(String empId) {
        this.assignedToEmpId = empId;
    }

    public String getAssignedToEmpId() {
        return assignedToEmpId;
    }

    @Override
    public String toString() {
        return "Loan{" +
                "id=" + id +
                ", customerId=" + getCustomerId() +
                ", loanType='" + loanType + '\'' +
                ", amountRequested=" + amountRequested +
                ", status='" + status + '\'' +
                ", appliedAt=" + appliedAt +
                ", reviewedBy='" + reviewedBy + '\'' +
                ", reviewedAt=" + reviewedAt +
                ", assignedToEmpId='" + assignedToEmpId + '\'' +
                '}';
    }

    // equals and hashCode can be added if needed for collections
}