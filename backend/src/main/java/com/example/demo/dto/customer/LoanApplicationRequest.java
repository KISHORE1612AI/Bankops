package com.example.demo.dto.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * DTO for loan application request (customer-side).
 */
public class LoanApplicationRequest {

    @NotBlank(message = "Loan type is required")
    private String loanType;

    @NotNull(message = "Amount requested is required")
    @Positive(message = "Amount requested must be positive")
    private BigDecimal amountRequested;

    public LoanApplicationRequest() {}

    public LoanApplicationRequest(String loanType, BigDecimal amountRequested) {
        this.loanType = loanType;
        this.amountRequested = amountRequested;
    }

    public String getLoanType() {
        return loanType;
    }

    public void setLoanType(String loanType) {
        this.loanType = loanType;
    }

    public BigDecimal getAmountRequested() {
        return amountRequested;
    }

    public void setAmountRequested(BigDecimal amountRequested) {
        this.amountRequested = amountRequested;
    }
}