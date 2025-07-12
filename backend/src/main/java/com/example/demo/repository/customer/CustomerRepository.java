package com.example.demo.repository.customer;

import com.example.demo.model.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // Find by email
    Optional<Customer> findByEmail(String email);

    // Case-insensitive email search
    Optional<Customer> findByEmailIgnoreCase(String email);

    // Find by account number
    Optional<Customer> findByAccountNumber(String accountNumber);

    // Find by email and account status
    Optional<Customer> findByEmailAndAccountStatus(String email, String status);

    // Find all customers with a specific account status
    List<Customer> findAllByAccountStatus(String status);

    // Check if account number already exists (for uniqueness)
    boolean existsByAccountNumber(String accountNumber);

    // Find by phone (for fund transfer validation, etc)
    Optional<Customer> findByPhone(String phone);

    // Find by full name (for fund transfer recipient validation)
    List<Customer> findByFullName(String fullName);

    // Find by email or phone (for login or validation)
    Optional<Customer> findByEmailOrPhone(String email, String phone);

    // ========= Super Admin controls: status-based queries =========

    // Find all customers by status (for activation/deactivation)
    List<Customer> findAllByStatus(String status);

    // Find by account status and user status (for advanced filtering)
    List<Customer> findAllByAccountStatusAndStatus(String accountStatus, String status);

    // Find by email and user status
    Optional<Customer> findByEmailAndStatus(String email, String status);

    // ========= Dashboard/statistics =========

    // Count customers by user status (used in dashboard stats)
    long countByStatus(String status);
}