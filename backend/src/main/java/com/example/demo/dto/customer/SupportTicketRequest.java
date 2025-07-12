package com.example.demo.dto.customer;

/**
 * Request DTO for creating or updating support tickets.
 * Used by both customers (to create) and support agents (to resolve).
 */
public class SupportTicketRequest {
    private String issueType;
    private String description;

    // For resolution by support agent
    private String resolutionNote;
    private String status; // For agent to mark as RESOLVED/CLOSED, etc.

    public SupportTicketRequest() {}

    // For customer ticket creation
    public SupportTicketRequest(String issueType, String description) {
        this.issueType = issueType;
        this.description = description;
    }

    // For agent ticket resolution/update
    public SupportTicketRequest(String issueType, String description, String resolutionNote, String status) {
        this.issueType = issueType;
        this.description = description;
        this.resolutionNote = resolutionNote;
        this.status = status;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}