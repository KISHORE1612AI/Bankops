package com.example.demo.dto.customer;

import com.example.demo.model.customer.SupportTicket;
import java.time.LocalDateTime;

/**
 * DTO for support/issue tickets for the customer and support dashboards.
 */
public class TicketDto {
    private Long id;
    private Long customerId;
    private String issueType;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private String handledBy;
    private LocalDateTime handledAt;
    private String resolutionNote; // ADDED FOR SUPPORT AGENT RESOLUTION

    public TicketDto() {}

    public TicketDto(Long id, Long customerId, String issueType, String description, String status,
                     LocalDateTime createdAt, String handledBy, LocalDateTime handledAt, String resolutionNote) {
        this.id = id;
        this.customerId = customerId;
        this.issueType = issueType;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.handledBy = handledBy;
        this.handledAt = handledAt;
        this.resolutionNote = resolutionNote;
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getHandledBy() { return handledBy; }
    public void setHandledBy(String handledBy) { this.handledBy = handledBy; }

    public LocalDateTime getHandledAt() { return handledAt; }
    public void setHandledAt(LocalDateTime handledAt) { this.handledAt = handledAt; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    // --- Robust static factory method ---
    public static TicketDto fromEntity(SupportTicket ticket) {
        if (ticket == null) return null;
        return new TicketDto(
            ticket.getId(),
            ticket.getCustomer() != null ? ticket.getCustomer().getId() : null,
            ticket.getIssueType(),
            ticket.getDescription(),
            ticket.getStatus(),
            ticket.getCreatedAt(),
            ticket.getHandledBy(),
            ticket.getHandledAt(),
            ticket.getResolutionNote() // ensure this is present in your entity!
        );
    }
}