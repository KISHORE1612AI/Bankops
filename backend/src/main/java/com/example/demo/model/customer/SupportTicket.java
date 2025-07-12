package com.example.demo.model.customer;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "support_ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;

    @NotNull(message = "Issue type is required")
    @Size(min = 1, max = 100)
    @Column(nullable = false, length = 100)
    private String issueType;

    @NotNull(message = "Description is required")
    @Size(min = 10, max = 1000, message = "Description must be at least 10 characters")
    @Column(nullable = false, length = 1000)
    private String description;

    @NotNull(message = "Status is required")
    @Size(min = 1, max = 50)
    @Column(nullable = false, length = 50)
    private String status;

    @NotNull(message = "Created date is required")
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Size(max = 100)
    @Column(length = 100)
    private String handledBy;

    private LocalDateTime handledAt;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;

    // For Super Admin ticket assignment
    @Column(name = "assigned_to_emp_id", length = 50)
    private String assignedToEmpId;

    // --- Ticket status enum ---
    public enum TicketStatus {
        OPEN,
        IN_PROGRESS,
        ON_HOLD,
        RESOLVED,
        CLOSED,
        REOPENED
    }

    // --- Pre-persist hook to set createdAt and default status ---
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TicketStatus.OPEN.name();
        }
    }

    // --- Method to check if ticket is handled ---
    public boolean isHandled() {
        return handledBy != null && handledAt != null;
    }

    // --- Method to check if ticket is resolved ---
    public boolean isResolved() {
        return TicketStatus.RESOLVED.name().equals(this.status) ||
                TicketStatus.CLOSED.name().equals(this.status);
    }

    // --- Method to get ticket age in hours ---
    public long getTicketAgeInHours() {
        LocalDateTime endTime = handledAt != null ? handledAt : LocalDateTime.now();
        return Duration.between(createdAt, endTime).toHours();
    }

    // --- Method to assign ticket to support staff or Super Admin assignment ---
    public void assignTo(String empId) {
        this.assignedToEmpId = empId;
        this.status = TicketStatus.IN_PROGRESS.name();
        if (this.handledAt == null) {
            this.handledAt = LocalDateTime.now();
        }
    }

    // --- Method to update ticket status and resolution note ---
    public void updateStatus(TicketStatus newStatus, String resolutionNote) {
        this.status = newStatus.name();
        if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) {
            this.handledAt = LocalDateTime.now();
            this.resolutionNote = resolutionNote;
        }
    }

    // --- Overload for backward compatibility ---
    public void updateStatus(TicketStatus newStatus) {
        updateStatus(newStatus, null);
    }

    // --- Method to reopen ticket ---
    public void reopen() {
        this.status = TicketStatus.REOPENED.name();
        this.handledAt = LocalDateTime.now();
    }

    // --- Method to validate ticket ---
    public boolean isValid() {
        return customer != null
                && issueType != null
                && description != null
                && description.length() >= 10
                && status != null;
    }

    // --- Custom getter for customerId (to be used in DTOs/services) ---
    public Long getCustomerId() {
        return customer != null ? customer.getId() : null;
    }

    // --- Method to get ticket priority based on age and status ---
    public String getPriority() {
        long ageInHours = getTicketAgeInHours();
        if (TicketStatus.RESOLVED.name().equals(status) ||
                TicketStatus.CLOSED.name().equals(status)) {
            return "LOW";
        }
        if (ageInHours > 72) return "HIGH";
        if (ageInHours > 24) return "MEDIUM";
        return "LOW";
    }

    // --- Super Admin assignment/reassignment methods ---
    public String getAssignedToEmpId() {
        return assignedToEmpId;
    }
}