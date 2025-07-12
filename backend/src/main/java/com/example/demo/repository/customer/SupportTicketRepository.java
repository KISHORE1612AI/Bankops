package com.example.demo.repository.customer;

import com.example.demo.model.customer.SupportTicket;
import com.example.demo.model.customer.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    // Find all tickets for a specific customer
    List<SupportTicket> findByCustomer(Customer customer);

    // Find all tickets by status (for support dashboard)
    List<SupportTicket> findByStatus(String status);
    
    // Find all tickets for a customer ordered by creation date
    List<SupportTicket> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    // Paginated queries for better performance
    Page<SupportTicket> findByCustomer_Id(Long customerId, Pageable pageable);
    Page<SupportTicket> findByStatus(String status, Pageable pageable);

    // Find tickets by issue type
    List<SupportTicket> findByCustomer_IdAndIssueType(Long customerId, String issueType);
    List<SupportTicket> findByIssueTypeAndStatus(String issueType, String status);

    // Find tickets by date range
    List<SupportTicket> findByCustomer_IdAndCreatedAtBetween(
        Long customerId, 
        LocalDateTime startDate, 
        LocalDateTime endDate
    );

    // Find unassigned tickets
    List<SupportTicket> findByStatusAndHandledByIsNull(String status);
    
    // Find tickets handled by specific support staff
    List<SupportTicket> findByHandledBy(String handledBy);

    // Find urgent or priority tickets
    List<SupportTicket> findByStatusInAndHandledByIsNull(List<String> statuses);

    // Find recently updated tickets
    List<SupportTicket> findByHandledAtGreaterThanOrderByHandledAtDesc(LocalDateTime since);

    // --- NEW: Paginated and filtered ticket search for support dashboard ---
    Page<SupportTicket> findByIssueTypeAndStatus(String issueType, String status, Pageable pageable);

    // --- NEW: All tickets with optional filtering by status, issueType, handledBy (for dashboard search) ---
    Page<SupportTicket> findByStatusAndIssueType(String status, String issueType, Pageable pageable);
    Page<SupportTicket> findByStatusAndHandledBy(String status, String handledBy, Pageable pageable);

    // --- NEW: Find by multiple statuses and handledBy (for dashboard filtering) ---
    Page<SupportTicket> findByStatusInAndHandledBy(List<String> statuses, String handledBy, Pageable pageable);

    // --- NEW: Find tickets resolved since a specific date (for reporting/statistics) ---
    Page<SupportTicket> findByStatusAndHandledAtAfter(String status, LocalDateTime handledAt, Pageable pageable);

    // --- WEEKLY STATS: Count tickets grouped by actual date (for dashboard chart) ---
    @Query(
        value = "SELECT DATE(createdAt) AS day, COUNT(*) AS count " +
                "FROM support_ticket " +
                "WHERE createdAt >= :fromDate " +
                "GROUP BY day " +
                "ORDER BY day",
        nativeQuery = true
    )
    List<Object[]> countTicketsPerDate(@Param("fromDate") java.sql.Date fromDate);

    // === Super Admin: Assignment/reassignment features ===

    // Find tickets assigned to a specific employee (support staff) by empId
    List<SupportTicket> findByAssignedToEmpId(String assignedToEmpId);

    // Find tickets by status and assigned employee
    List<SupportTicket> findByStatusAndAssignedToEmpId(String status, String assignedToEmpId);

    // Paginated assignment queries
    Page<SupportTicket> findByAssignedToEmpId(String assignedToEmpId, Pageable pageable);
    Page<SupportTicket> findByStatusAndAssignedToEmpId(String status, String assignedToEmpId, Pageable pageable);

    // === Dashboard/statistics ===

    // Count tickets by status (for dashboard stats)
    long countByStatus(String status);
}