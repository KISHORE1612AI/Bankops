package com.example.demo.dto.auditor;

import java.math.BigDecimal;
import java.util.List;
import com.example.demo.dto.customer.TransactionDto;

public class AuditorDashboardDTO {

    private long totalCustomers;
    private long totalTransactions;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private List<TransactionDto> recentTransactions;

    // ─── 1) No-arg constructor ──────────────────────────────────────────────
    // This is required for Jackson (and for any case where Spring
    // needs to instantiate the DTO before setting fields)
    public AuditorDashboardDTO() { 
        // You can leave this empty or initialize defaults if you like
    }

    // ─── 2) Convenience 3-arg constructor ─────────────────────────────────
    // If you ever want to build the DTO in one call, you can use this.
    // Notice that totalWithdrawals is initialized to zero here.
    public AuditorDashboardDTO(long totalCustomers,
                               BigDecimal totalDeposits,
                               long totalTransactions) {
        this.totalCustomers    = totalCustomers;
        this.totalDeposits     = totalDeposits;
        this.totalWithdrawals  = BigDecimal.ZERO;
        this.totalTransactions = totalTransactions;
        // recentTransactions will remain null until set explicitly
    }

    // ─── Public Getters & Setters ──────────────────────────────────────────
    public long getTotalCustomers() {
        return totalCustomers;
    }

    public void setTotalCustomers(long totalCustomers) {
        this.totalCustomers = totalCustomers;
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(BigDecimal totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public BigDecimal getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public void setTotalWithdrawals(BigDecimal totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }

    public List<TransactionDto> getRecentTransactions() {
        return recentTransactions;
    }

    public void setRecentTransactions(List<TransactionDto> recentTransactions) {
        this.recentTransactions = recentTransactions;
    }
}
