package com.example.demo.repository.customer;

import com.example.demo.model.customer.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, TransactionRepositoryCustom {

    // --- Permanent solution: Only show relevant transactions per user ---
    @Query("SELECT t FROM Transaction t WHERE " +
           "(t.customer.id = :userId AND (t.type <> 'TRANSFER_IN' OR t.recipient IS NULL)) " +
           "OR (t.recipient.id = :userId AND t.type = 'TRANSFER_IN') " +
           "ORDER BY t.timestamp DESC")
    List<Transaction> findRelevantTransactionsForUser(@Param("userId") Long userId);

    // Existing methods (legacy/used elsewhere)
    List<Transaction> findByCustomer_Id(Long customerId);

    List<Transaction> findByCustomer_IdOrderByTimestampDesc(Long customerId);

    List<Transaction> findByCustomer_IdOrRecipient_IdOrderByTimestampDesc(Long customerId, Long recipientId);

    List<Transaction> findAllByOrderByTimestampDesc();

    Page<Transaction> findByCustomer_Id(Long customerId, Pageable pageable);

    Page<Transaction> findByCustomer_IdOrRecipient_Id(Long customerId, Long recipientId, Pageable pageable);

    List<Transaction> findByCustomer_IdAndTimestampBetweenOrderByTimestampDesc(
        Long customerId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );

    List<Transaction> findByCustomer_IdAndTypeOrderByTimestampDesc(Long customerId, String type);

    List<Transaction> findByCustomer_IdAndStatusOrderByTimestampDesc(Long customerId, String status);

    // ====== AUDITOR DASHBOARD ADVANCED SEARCH ======
    /**
     * Advanced search for transactions (auditor dashboard): supports
     * - date (timestamp's date part)
     * - customer (by customer email or id)
     * - type (txn type)
     * - status
     * All filters are optional. If not given, returns all.
     *
     * IMPORTANT:
     * Never pass an empty string as :date, :customer, :type, or :status.
     * Always pass either a valid value or null.
     */
    @Query("""
        SELECT t FROM Transaction t
        LEFT JOIN Customer c ON t.customer.id = c.id
        WHERE
            (:date IS NULL OR FUNCTION('DATE', t.timestamp) = :date)
            AND (
                :customer IS NULL
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :customer, '%'))
                OR CAST(c.id AS string) = :customer
            )
            AND (:type IS NULL OR t.type = :type)
            AND (:status IS NULL OR t.status = :status)
        ORDER BY t.timestamp DESC
    """)
    Page<Transaction> advancedSearch(
            @Param("date") String date,
            @Param("customer") String customer,
            @Param("type") String type,
            @Param("status") String status,
            Pageable pageable
    );
}