package com.example.demo.service.employee;

import com.example.demo.dto.admin.*;
import com.example.demo.dto.customer.LoanDto;
import com.example.demo.dto.customer.TransactionDto;
import com.example.demo.model.employee.SuperAdmin;
import com.example.demo.model.customer.Loan;
import com.example.demo.model.customer.Transaction;
import com.example.demo.model.customer.Customer;
import com.example.demo.repository.employee.SuperAdminRepository;
import com.example.demo.repository.customer.LoanRepository;
import com.example.demo.repository.customer.TransactionRepository;
import com.example.demo.repository.customer.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SuperAdminService {

    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoanRepository loanRepository;
    private final TransactionRepository transactionRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public SuperAdminService(
            SuperAdminRepository superAdminRepository,
            PasswordEncoder passwordEncoder,
            LoanRepository loanRepository,
            TransactionRepository transactionRepository,
            CustomerRepository customerRepository
    ) {
        this.superAdminRepository = superAdminRepository;
        this.passwordEncoder = passwordEncoder;
        this.loanRepository = loanRepository;
        this.transactionRepository = transactionRepository;
        this.customerRepository = customerRepository;
    }

    // --- SuperAdmin core management ---
    public List<SuperAdminDto> getAll() {
        return superAdminRepository.findAll().stream().map(this::toDto).toList();
    }

    public Optional<SuperAdminDto> getById(Long id) {
        return superAdminRepository.findById(id).map(this::toDto);
    }

    public Optional<SuperAdminDto> getByEmail(String email) {
        return superAdminRepository.findByEmail(email).map(this::toDto);
    }

    @Transactional
    public SuperAdminDto create(SuperAdminCreateRequest req) {
        if (superAdminRepository.existsByEmail(req.getEmail()))
            throw new RuntimeException("SuperAdmin with this email already exists");

        SuperAdmin admin = SuperAdmin.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .status(SuperAdmin.Status.ACTIVE)
                .build();
        admin = superAdminRepository.save(admin);
        return toDto(admin);
    }

    @Transactional
    public SuperAdminDto update(Long id, SuperAdminUpdateRequest req) {
        SuperAdmin admin = superAdminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SuperAdmin not found"));
        if (req.getName() != null) admin.setName(req.getName());
        if (req.getPhone() != null) admin.setPhone(req.getPhone());
        if (req.getStatus() != null)
            admin.setStatus(SuperAdmin.Status.valueOf(req.getStatus()));
        admin = superAdminRepository.save(admin);
        return toDto(admin);
    }

    @Transactional
    public boolean delete(Long id) {
        if (!superAdminRepository.existsById(id)) return false;
        superAdminRepository.deleteById(id);
        return true;
    }

    private SuperAdminDto toDto(SuperAdmin admin) {
        SuperAdminDto dto = new SuperAdminDto();
        dto.setId(admin.getId());
        dto.setName(admin.getName());
        dto.setEmail(admin.getEmail());
        dto.setPhone(admin.getPhone());
        dto.setStatus(admin.getStatus() != null ? admin.getStatus().name() : null);
        dto.setCreatedAt(admin.getCreatedAt());
        return dto;
    }

    // ========== Loan & Transaction APIs for Super Admin ==========

    // --- Loans ---
    public List<LoanDto> getAllLoans() {
        List<Loan> loans = loanRepository.findAll();
        return loans.stream().map(this::toLoanDto).toList();
    }

    public Optional<LoanDto> getLoanById(Long id) {
        return loanRepository.findById(id).map(this::toLoanDto);
    }

    private LoanDto toLoanDto(Loan loan) {
        LoanDto dto = new LoanDto();
        dto.setId(loan.getId());
        dto.setCustomerId(loan.getCustomerId());
        // Fetch customer details for name/email if available
        if (loan.getCustomerId() != null) {
            Optional<Customer> customerOpt = customerRepository.findById(loan.getCustomerId());
            customerOpt.ifPresent(customer -> {
                dto.setCustomerName(customer.getName());
                dto.setCustomerEmail(customer.getEmail());
            });
        }
        dto.setLoanType(loan.getLoanType());
        dto.setAmountRequested(loan.getAmountRequested());
        dto.setStatus(loan.getStatus()); // <-- FIXED: No .name()
        dto.setAppliedAt(loan.getAppliedAt());
        dto.setReviewedBy(loan.getReviewedBy());
        dto.setReviewedAt(loan.getReviewedAt());
        return dto;
    }

    // --- Transactions ---
    public List<TransactionDto> getAllTransactions() {
        List<Transaction> txns = transactionRepository.findAll();
        return txns.stream().map(this::toTransactionDto).toList();
    }

    public Optional<TransactionDto> getTransactionById(Long id) {
        return transactionRepository.findById(id).map(this::toTransactionDto);
    }

    private TransactionDto toTransactionDto(Transaction txn) {
        TransactionDto dto = new TransactionDto();
        dto.setId(txn.getId());
        dto.setCustomerId(txn.getCustomerId());
        dto.setRecipientId(txn.getRecipientId());
        dto.setTimestamp(txn.getTimestamp());
        dto.setDescription(txn.getDescription());
        dto.setAmount(txn.getAmount());
        dto.setType(txn.getType());
        dto.setStatus(txn.getStatus()); // <-- FIXED: No .name()
        return dto;
    }
}