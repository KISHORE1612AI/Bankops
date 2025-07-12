package com.example.demo.controller.loan;

import com.example.demo.dto.customer.LoanDto;
import com.example.demo.model.customer.Loan;
import com.example.demo.service.customer.LoanService;
import com.example.demo.model.customer.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for Loan Officer dashboard functions:
 * - View loan requests by status
 * - Approve/Reject/Process/Cancel loans
 * - Dashboard stats (counts, chart data)
 */
@RestController
@RequestMapping("/api/loans")
@PreAuthorize("hasAnyRole('LOAN_OFFICER','SUPER_ADMIN')")
public class LoanController {

    private final LoanService loanService;

    @Autowired
    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    // --- List all loans by status (for dashboard tables/cards) ---
    @GetMapping
    public List<LoanDto> getLoansByStatus(@RequestParam(required = false) String status,
                                          @RequestParam(required = false) String reviewedBy) {
        List<Loan> loans;
        // If reviewedBy is provided (for assigned dashboard), use the JOIN FETCH method for reviewed loans
        if (reviewedBy != null && !reviewedBy.isEmpty() && status != null && !status.isEmpty()) {
            loans = loanService.getLoansReviewedBy(status, reviewedBy);
        } else {
            // Default: by status only (uses JOIN FETCH under the hood)
            loans = (status == null || status.isEmpty())
                    ? loanService.getLoansByStatus("PENDING")
                    : loanService.getLoansByStatus(status);
        }
        // Use DTO mapping to avoid LazyInitializationException
        return loans.stream().map(this::toLoanDto).collect(Collectors.toList());
    }

    // --- Get a single loan by ID ---
    @GetMapping("/{loanId}")
    public ResponseEntity<LoanDto> getLoanById(@PathVariable Long loanId) {
        Optional<Loan> loanOpt = loanService.getLoanById(loanId);
        return loanOpt.map(loan -> ResponseEntity.ok(toLoanDto(loan)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // --- Approve a loan ---
    @PutMapping("/{loanId}/approve")
    public ResponseEntity<LoanDto> approveLoan(@PathVariable Long loanId, Principal principal) {
        Loan loan = loanService.reviewLoan(loanId, principal.getName(), Loan.LoanStatus.APPROVED.name());
        return ResponseEntity.ok(toLoanDto(loan));
    }

    // --- Reject a loan ---
    @PutMapping("/{loanId}/reject")
    public ResponseEntity<LoanDto> rejectLoan(@PathVariable Long loanId, Principal principal) {
        Loan loan = loanService.reviewLoan(loanId, principal.getName(), Loan.LoanStatus.REJECTED.name());
        return ResponseEntity.ok(toLoanDto(loan));
    }

    // --- Mark loan as 'IN PROCESS/UNDER_REVIEW' ---
    @PutMapping("/{loanId}/process")
    public ResponseEntity<LoanDto> processLoan(@PathVariable Long loanId, Principal principal) {
        Loan loan = loanService.markLoanInProcess(loanId, principal.getName());
        return ResponseEntity.ok(toLoanDto(loan));
    }

    // --- Cancel a loan (optional) ---
    @PutMapping("/{loanId}/cancel")
    public ResponseEntity<LoanDto> cancelLoan(@PathVariable Long loanId, Principal principal) {
        Loan loan = loanService.cancelLoan(loanId, principal.getName());
        return ResponseEntity.ok(toLoanDto(loan));
    }

    // --- Get dashboard stats: counts by status (for cards) ---
    @GetMapping("/dashboard/counts")
    public ResponseEntity<?> getLoanCountsByStatus() {
        return ResponseEntity.ok(loanService.getLoanCountsByStatus());
    }

    // --- Get dashboard stats: monthly applications chart ---
    @GetMapping("/dashboard/monthly-applications")
    public ResponseEntity<?> getMonthlyApplications(@RequestParam(defaultValue = "6") int monthsBack) {
        return ResponseEntity.ok(loanService.getMonthlyApplications(monthsBack));
    }

    // --- Helper: convert Loan to LoanDto with customer info ---
    private LoanDto toLoanDto(Loan loan) {
        // Avoid LazyInitializationException by checking if customer is initialized
        Customer customer = loan.getCustomer();
        return new LoanDto(
                loan.getId(),
                (customer != null) ? customer.getId() : null,
                (customer != null) ? customer.getFullName() : null,
                (customer != null) ? customer.getEmail() : null,
                loan.getLoanType(),
                loan.getAmountRequested(),
                loan.getStatus(),
                loan.getAppliedAt(),
                loan.getReviewedBy(),
                loan.getReviewedAt()
        );
    }
}