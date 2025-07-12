package com.example.demo.repository.customer;

import com.example.demo.model.customer.Loan;
import com.example.demo.model.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    // Find all loans for a specific customer
    List<Loan> findByCustomer(Customer customer);

    // Find all loans by status
    List<Loan> findByStatus(String status);

    // Find all loans for a specific customer, ordered by appliedAt descending
    List<Loan> findByCustomer_IdOrderByAppliedAtDesc(Long customerId);

    // Paginated queries
    Page<Loan> findByCustomer(Customer customer, Pageable pageable);
    Page<Loan> findByStatus(String status, Pageable pageable);

    // Find loans by type
    List<Loan> findByCustomer_IdAndLoanType(Long customerId, String loanType);
    List<Loan> findByLoanTypeAndStatus(String loanType, String status);

    // Find loans by date range
    List<Loan> findByCustomer_IdAndAppliedAtBetween(
        Long customerId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    // Find loans by amount range
    List<Loan> findByCustomer_IdAndAmountRequestedBetween(
        Long customerId,
        BigDecimal minAmount,
        BigDecimal maxAmount
    );

    // Find pending loans (not yet reviewed)
    List<Loan> findByStatusAndReviewedByIsNull(String status);

    // Find loans reviewed by specific officer
    List<Loan> findByReviewedBy(String reviewedBy);

    // === Super Admin: Loan assignment operations ===

    // Find loans assigned to a specific employee (Loan Officer) by empId
    List<Loan> findByAssignedToEmpId(String assignedToEmpId);

    // Find loans by status and assigned employee
    List<Loan> findByStatusAndAssignedToEmpId(String status, String assignedToEmpId);

    // === Dashboard/Stats Queries ===

    // Count loans by status
    @Query("SELECT l.status, COUNT(l) FROM Loan l GROUP BY l.status")
    List<Object[]> countLoansByStatus();

    // Count monthly loan applications for the last N months
    @Query("SELECT FUNCTION('YEAR', l.appliedAt) as yr, FUNCTION('MONTH', l.appliedAt) as mo, COUNT(l) " +
           "FROM Loan l WHERE l.appliedAt >= :fromDate GROUP BY yr, mo ORDER BY yr, mo")
    List<Object[]> countMonthlyApplications(@Param("fromDate") LocalDateTime fromDate);

    // ==== Dashboard: Total loan assets ====
    @Query("SELECT SUM(l.amountRequested) FROM Loan l")
    BigDecimal sumAllLoanAmounts();

    // ==== Eager loading to avoid LazyInitializationException ====

    // Fetch loans by status with customer JOIN FETCH (for dashboard tables)
    @Query("SELECT l FROM Loan l JOIN FETCH l.customer WHERE l.status = :status")
    List<Loan> findByStatusWithCustomer(@Param("status") String status);

    // Fetch single loan with customer
    @Query("SELECT l FROM Loan l JOIN FETCH l.customer WHERE l.id = :loanId")
    Optional<Loan> findByIdWithCustomer(@Param("loanId") Long loanId);

    // Fetch loans reviewed by officer, with customer (for assigned dashboard)
    @Query("SELECT l FROM Loan l JOIN FETCH l.customer WHERE l.status = :status AND l.reviewedBy = :reviewedBy")
    List<Loan> findByStatusAndReviewedByWithCustomer(@Param("status") String status, @Param("reviewedBy") String reviewedBy);

    // =========================
    // REMOVED: BRANCH DASHBOARD queries
    // =========================
}