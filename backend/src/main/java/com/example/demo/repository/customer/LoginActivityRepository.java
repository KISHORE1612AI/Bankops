package com.example.demo.repository.customer;

import com.example.demo.model.customer.LoginActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {

    // Find all login activities for a specific customer ID
    Page<LoginActivity> findByCustomerId(Long customerId, Pageable pageable);

    // Find all activities for a specific date (uses timestamp's date part)
    @Query("SELECT l FROM LoginActivity l WHERE DATE(l.timestamp) = :date")
    Page<LoginActivity> findByDate(@Param("date") LocalDate date, Pageable pageable);

    // Find all activities for a customer email (requires join with Customer)
    @Query("SELECT l FROM LoginActivity l JOIN Customer c ON l.customerId = c.id WHERE LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    Page<LoginActivity> findByCustomerEmail(@Param("email") String email, Pageable pageable);

    // Find all activities for both date and customer email
    @Query("SELECT l FROM LoginActivity l JOIN Customer c ON l.customerId = c.id WHERE DATE(l.timestamp) = :date AND LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%'))")
    Page<LoginActivity> findByDateAndCustomerEmail(@Param("date") LocalDate date, @Param("email") String email, Pageable pageable);

    // For legacy code - list-based queries (optionally keep)
    // List<LoginActivity> findByCustomerId(Long customerId);
    // List<LoginActivity> findTop10ByOrderByTimestampDesc();
    // List<LoginActivity> findTop100ByOrderByTimestampDesc();
}