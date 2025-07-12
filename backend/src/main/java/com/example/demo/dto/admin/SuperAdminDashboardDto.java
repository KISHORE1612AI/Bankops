package com.example.demo.dto.admin;

import java.math.BigDecimal;

public class SuperAdminDashboardDto {
    private long totalUsers;
    private long activeUsers;
    private long totalEmployees;
    private long totalLoans;
    private long pendingTickets;
    private BigDecimal totalAssets;

    // Getters and Setters

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getTotalEmployees() { return totalEmployees; }
    public void setTotalEmployees(long totalEmployees) { this.totalEmployees = totalEmployees; }

    public long getTotalLoans() { return totalLoans; }
    public void setTotalLoans(long totalLoans) { this.totalLoans = totalLoans; }

    public long getPendingTickets() { return pendingTickets; }
    public void setPendingTickets(long pendingTickets) { this.pendingTickets = pendingTickets; }

    public BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }
}