package com.example.demo.service.customer;

import com.example.demo.model.customer.Loan;
import com.example.demo.model.customer.Customer;
import com.example.demo.repository.customer.LoanRepository;
import com.example.demo.repository.customer.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public LoanService(LoanRepository loanRepository, CustomerRepository customerRepository) {
        this.loanRepository = loanRepository;
        this.customerRepository = customerRepository;
    }

    // Create/apply for a new loan (customer-side)
    @Transactional
    public Loan applyForLoan(Long customerId, Loan loan) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        loan.setCustomer(customer);
        loan.setStatus(Loan.LoanStatus.PENDING.name());
        loan.setAppliedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    // Get all loans for a customer
    public List<Loan> getLoansForCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        return loanRepository.findByCustomer(customer);
    }

    // Get all loans by status (for admin/officer) - Use join fetch to avoid LazyInitializationException
    @Transactional(readOnly = true)
    public List<Loan> getLoansByStatus(String status) {
        return loanRepository.findByStatusWithCustomer(status);
    }

    // Approve or reject a loan by ID (review)
    @Transactional
    public Loan reviewLoan(Long loanId, String reviewer, String newStatus) {
        // Use join fetch to avoid lazy error when further processing is needed
        Loan loan = loanRepository.findByIdWithCustomer(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        loan.setStatus(newStatus);
        loan.setReviewedBy(reviewer);
        loan.setReviewedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    // Mark loan as "IN_PROCESS"
    @Transactional
    public Loan markLoanInProcess(Long loanId, String reviewer) {
        Loan loan = loanRepository.findByIdWithCustomer(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        loan.setStatus(Loan.LoanStatus.UNDER_REVIEW.name());
        loan.setReviewedBy(reviewer);
        loan.setReviewedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    // Cancel a loan (optional)
    @Transactional
    public Loan cancelLoan(Long loanId, String reviewer) {
        Loan loan = loanRepository.findByIdWithCustomer(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        loan.setStatus(Loan.LoanStatus.CANCELLED.name());
        loan.setReviewedBy(reviewer);
        loan.setReviewedAt(LocalDateTime.now());
        return loanRepository.save(loan);
    }

    // Get a single loan
    public Optional<Loan> getLoanById(Long loanId) {
        return loanRepository.findByIdWithCustomer(loanId);
    }

    // Get all loans reviewed by a specific officer (empId or email) for dashboard: use JOIN FETCH
    @Transactional(readOnly = true)
    public List<Loan> getLoansReviewedBy(String status, String reviewedBy) {
        return loanRepository.findByStatusAndReviewedByWithCustomer(status, reviewedBy);
    }

    // === Dashboard Stats/Chart Methods ===

    // Count loans by status (for dashboard cards)
    public Map<String, Long> getLoanCountsByStatus() {
        List<Object[]> results = loanRepository.countLoansByStatus();
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : results) {
            counts.put((String) row[0], (Long) row[1]);
        }
        return counts;
    }

    // Get monthly application stats for the last N months (for chart)
    public List<Map<String, Object>> getMonthlyApplications(int monthsBack) {
        LocalDateTime fromDate = LocalDateTime.now().minusMonths(monthsBack);
        List<Object[]> results = loanRepository.countMonthlyApplications(fromDate);
        List<Map<String, Object>> stats = new ArrayList<>();
        for (Object[] row : results) {
            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            Map<String, Object> map = new HashMap<>();
            map.put("year", year);
            map.put("month", month);
            map.put("count", count);
            stats.add(map);
        }
        return stats;
    }
}