package com.example.demo.dto.admin;

import java.time.LocalDateTime;

public class SupportTicketDto {
    private Long id;
    private String customerName;
    private String issueType;
    private String status;
    private LocalDateTime createdAt;
    private String handledBy;

    public SupportTicketDto() {}

    public SupportTicketDto(Long id, String customerName, String issueType, String status, LocalDateTime createdAt, String handledBy) {
        this.id = id;
        this.customerName = customerName;
        this.issueType = issueType;
        this.status = status;
        this.createdAt = createdAt;
        this.handledBy = handledBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }
}