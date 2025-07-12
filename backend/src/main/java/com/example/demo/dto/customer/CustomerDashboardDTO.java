package com.example.demo.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregates all key information for displaying the customer dashboard.
 */
public class CustomerDashboardDTO {
    // Profile & Account Info
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String accountNumber;
    private BigDecimal accountBalance;
    private String accountStatus;
    private String address;

    // Recent Transactions
    private List<TransactionDto> recentTransactions;

    // Last Login
    private LocalDateTime lastLogin;

    // Open Loans and Tickets
    private List<LoanDto> openLoans;
    private List<TicketDto> openTickets;

    public CustomerDashboardDTO() {}

    public CustomerDashboardDTO(
            Long id,
            String fullName,
            String email,
            String phone,
            String accountNumber,
            BigDecimal accountBalance,
            String accountStatus,
            String address,
            List<TransactionDto> recentTransactions,
            LocalDateTime lastLogin,
            List<LoanDto> openLoans,
            List<TicketDto> openTickets
    ) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.accountNumber = accountNumber;
        this.accountBalance = accountBalance;
        this.accountStatus = accountStatus;
        this.address = address;
        this.recentTransactions = recentTransactions;
        this.lastLogin = lastLogin;
        this.openLoans = openLoans;
        this.openTickets = openTickets;
    }

    // Getters & Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public BigDecimal getAccountBalance() { return accountBalance; }
    public void setAccountBalance(BigDecimal accountBalance) { this.accountBalance = accountBalance; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public List<TransactionDto> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<TransactionDto> recentTransactions) { this.recentTransactions = recentTransactions; }

    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }

    public List<LoanDto> getOpenLoans() { return openLoans; }
    public void setOpenLoans(List<LoanDto> openLoans) { this.openLoans = openLoans; }

    public List<TicketDto> getOpenTickets() { return openTickets; }
    public void setOpenTickets(List<TicketDto> openTickets) { this.openTickets = openTickets; }
}