package com.example.demo.controller.customer;

import com.example.demo.dto.customer.TransferRequest;
import com.example.demo.dto.customer.LoanApplicationRequest;
import com.example.demo.dto.customer.SupportTicketRequest;
import com.example.demo.dto.customer.TicketDto;
import com.example.demo.model.customer.Customer;
import com.example.demo.model.customer.Loan;
import com.example.demo.model.customer.SupportTicket;
import com.example.demo.model.customer.Transaction;
import com.example.demo.repository.customer.*;
import com.example.demo.service.export.TransactionExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customer/dashboard")
@Tag(name = "Customer Dashboard API", description = "Operations for customer dashboard")
public class CustomerDashboardController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerDashboardController.class);

    @Autowired
    private CustomerRepository customerRepo;
    @Autowired
    private TransactionRepository transactionRepo;
    @Autowired
    private LoanRepository loanRepo;
    @Autowired
    private SupportTicketRepository ticketRepo;
    @Autowired(required = false)
    private TransactionExportService transactionExportService; // Service for PDF/CSV generation

    // Allowed loan types
    private static final Set<String> ALLOWED_LOAN_TYPES = Set.of(
            "Gold", "Home", "Education", "Vehicle", "Personal", "Business"
    );

    // Allowed support ticket issue types
    private static final Set<String> ALLOWED_ISSUE_TYPES = Set.of(
            "Login Issue", "Password Reset Issue", "Deposit Issue", "Transfer Issue",
            "Loan Application Issue", "Account Statement Issue", "Account Closure Request",
            "Card / Cheque Book Issue", "Technical Bug Report", "Other"
    );

    @Operation(summary = "Get customer profile", description = "Returns the dashboard profile information of the authenticated customer")
    @GetMapping("/profile")
    public ResponseEntity<?> getCustomerProfile(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));

            Map<String, Object> payload = new HashMap<>();
            payload.put("fullName", customer.getFullName());
            payload.put("email", customer.getEmail());
            payload.put("phone", customer.getPhone() != null ? customer.getPhone() : "");
            payload.put("accountNumber", customer.getAccountNumber() != null ? customer.getAccountNumber() : "");
            payload.put("accountBalance", customer.getAccountBalance() != null ? customer.getAccountBalance() : BigDecimal.ZERO);
            payload.put("accountStatus", customer.getAccountStatus() != null ? customer.getAccountStatus() : "ACTIVE");
            payload.put("address", customer.getAddress() != null ? customer.getAddress() : "");
            payload.put("lastLogin", customer.getLastLogin());

            logger.debug("Retrieved dashboard profile for customer: {}", customer.getEmail());
            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            logger.error("Error retrieving customer dashboard profile", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @Operation(summary = "Get customer transactions with filtering, sorting, pagination")
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        try {
            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));

            List<Transaction> transactions = transactionRepo.findFilteredTransactionsForUser(
                    customer.getId(), fromDate, toDate, type, status, sortBy, sortOrder
            );

            // Pagination manually (replace with JPA Pageable if desired)
            int fromIdx = Math.min(page * size, transactions.size());
            int toIdx = Math.min(fromIdx + size, transactions.size());
            List<Transaction> pagedList = transactions.subList(fromIdx, toIdx);

            List<Map<String, Object>> list = pagedList.stream().map(tx -> {
                Map<String, Object> txMap = new HashMap<>();
                txMap.put("id", tx.getId());
                txMap.put("customerId", tx.getCustomerId());
                txMap.put("recipientId", tx.getRecipientId());
                txMap.put("timestamp", tx.getTimestamp());
                txMap.put("description", tx.getDescription());
                txMap.put("amount", tx.getAmount());
                txMap.put("type", tx.getType());
                txMap.put("status", tx.getStatus());
                return txMap;
            }).collect(Collectors.toList());

            Map<String, Object> payload = new HashMap<>();
            payload.put("total", transactions.size());
            payload.put("page", page);
            payload.put("size", size);
            payload.put("transactions", list);

            logger.debug("Retrieved transactions for customer: {}", customer.getEmail());
            return ResponseEntity.ok(payload);
        } catch (Exception e) {
            logger.error("Error retrieving transactions", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @Operation(summary = "Export customer transactions to PDF or CSV")
    @GetMapping("/transactions/export")
    public ResponseEntity<?> exportTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        try {
            if (transactionExportService == null) {
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                        .body(Map.of("error", "Export service not available"));
            }

            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));

            List<Transaction> transactions = transactionRepo.findFilteredTransactionsForUser(
                    customer.getId(), fromDate, toDate, type, status, sortBy, sortOrder
            );

            String fileName = "transactions_" + customer.getId() + "_" + System.currentTimeMillis();
            ByteArrayInputStream in;
            HttpHeaders headers = new HttpHeaders();

            if ("csv".equalsIgnoreCase(format)) {
                in = transactionExportService.transactionsToCsv(transactions);
                headers.add("Content-Disposition", "attachment; filename=" + fileName + ".csv");
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("text/csv"))
                        .headers(headers)
                        .body(new InputStreamResource(in));
            } else {
                in = transactionExportService.transactionsToPdf(transactions, customer);
                headers.add("Content-Disposition", "attachment; filename=" + fileName + ".pdf");
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .headers(headers)
                        .body(new InputStreamResource(in));
            }
        } catch (Exception e) {
            logger.error("Error exporting transactions", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Export failed"));
        }
    }

    @Operation(summary = "Get customer loans", description = "Returns all loans for the customer")
    @GetMapping("/loans")
    public ResponseEntity<?> getOpenLoans(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));

            List<Loan> loans = loanRepo.findByCustomer_IdOrderByAppliedAtDesc(customer.getId());

            List<Map<String, Object>> loanList = loans.stream()
                    .filter(loan -> loan.getCustomer() != null)
                    .map(loan -> {
                        Map<String, Object> loanMap = new HashMap<>();
                        loanMap.put("id", loan.getId());
                        loanMap.put("customerId", loan.getCustomer().getId());
                        loanMap.put("loanType", loan.getLoanType());
                        loanMap.put("amountRequested", loan.getAmountRequested());
                        loanMap.put("status", loan.getStatus());
                        loanMap.put("appliedAt", loan.getAppliedAt());
                        loanMap.put("reviewedBy", loan.getReviewedBy());
                        loanMap.put("reviewedAt", loan.getReviewedAt());
                        return loanMap;
                    })
                    .collect(Collectors.toList());

            logger.debug("Retrieved {} loans for customer: {}",
                    loanList.size(), customer.getEmail());
            return ResponseEntity.ok(loanList);
        } catch (Exception e) {
            logger.error("Error retrieving customer loans", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @Operation(summary = "Get support tickets", description = "Returns all support tickets for the customer")
    @GetMapping("/tickets")
    public ResponseEntity<?> getOpenTickets(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));

            List<SupportTicket> tickets = ticketRepo.findByCustomer_IdOrderByCreatedAtDesc(customer.getId());

            // Use robust DTO mapping to avoid lazy loading and NPEs
            List<TicketDto> ticketList = tickets.stream()
                    .map(TicketDto::fromEntity)
                    .collect(Collectors.toList());

            logger.debug("Retrieved {} tickets for customer: {}",
                    ticketList.size(), customer.getEmail());
            return ResponseEntity.ok(ticketList);
        } catch (Exception e) {
            logger.error("Error retrieving support tickets", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @Operation(summary = "Deposit funds", description = "Deposit money into logged-in customer's own account")
    @PostMapping("/deposit")
    public ResponseEntity<?> depositFunds(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam BigDecimal amount
    ) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Deposit amount must be positive"));
        }
        try {
            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));
            // Update balance
            BigDecimal newBalance = customer.getAccountBalance() == null
                ? amount
                : customer.getAccountBalance().add(amount);
            customer.setAccountBalance(newBalance);
            customerRepo.save(customer);
            // (Optional) Record as a transaction
            Transaction depositTx = Transaction.builder()
                    .customer(customer)
                    .recipient(null)
                    .amount(amount)
                    .type("DEPOSIT")
                    .status("SUCCESS")
                    .description("Amount deposited")
                    .timestamp(LocalDateTime.now())
                    .build();
            transactionRepo.save(depositTx);

            logger.info("Deposit successful for customer {}. Amount: {}. New balance: {}", customer.getEmail(), amount, newBalance);
            return ResponseEntity.ok(Map.of(
                    "balance", newBalance,
                    "message", "Deposit successful"
            ));
        } catch (Exception e) {
            logger.error("Deposit failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Deposit failed"));
        }
    }

    @Operation(summary = "Transfer funds", description = "Transfer money to another registered customer by account number")
    @PostMapping("/transfer")
    @Transactional // Ensures database atomicity for both accounts
    public ResponseEntity<?> transferFunds(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody TransferRequest transferRequest
    ) {
        String recipientAccountNumber = transferRequest.getRecipientAccountNumber();
        BigDecimal amount = transferRequest.getAmount();

        if (recipientAccountNumber == null || recipientAccountNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Recipient account number is required"));
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transfer amount must be positive"));
        }
        try {
            Customer sender = customerRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));
            if (sender.getAccountNumber() == null || sender.getAccountNumber().equals(recipientAccountNumber)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid recipient account"));
            }

            // Find recipient by account number
            Optional<Customer> recipientOpt = customerRepo.findByAccountNumber(recipientAccountNumber);
            if (recipientOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Recipient not found"));
            }
            Customer recipient = recipientOpt.get();

            // Validation: sender balance
            if (sender.getAccountBalance() == null || sender.getAccountBalance().compareTo(amount) < 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Insufficient balance"));
            }

            // Update balances atomically
            sender.setAccountBalance(sender.getAccountBalance().subtract(amount));
            recipient.setAccountBalance(
                    recipient.getAccountBalance() == null ? amount : recipient.getAccountBalance().add(amount)
            );
            customerRepo.save(sender);
            customerRepo.save(recipient);

            // Record transactions for both parties
            Transaction senderTx = Transaction.builder()
                    .customer(sender)
                    .recipient(recipient)
                    .amount(amount)
                    .type("TRANSFER_OUT")
                    .status("SUCCESS")
                    .description("Transferred to account: " + recipient.getAccountNumber())
                    .timestamp(LocalDateTime.now())
                    .build();
            transactionRepo.save(senderTx);

            Transaction recipientTx = Transaction.builder()
                    .customer(recipient)
                    .recipient(sender)
                    .amount(amount)
                    .type("TRANSFER_IN")
                    .status("SUCCESS")
                    .description("Received from account: " + sender.getAccountNumber())
                    .timestamp(LocalDateTime.now())
                    .build();
            transactionRepo.save(recipientTx);

            logger.info("Transfer: {} -> {} Amount: {}", sender.getEmail(), recipient.getEmail(), amount);
            return ResponseEntity.ok(Map.of(
                    "balance", sender.getAccountBalance(),
                    "message", "Transfer successful"
            ));
        } catch (Exception e) {
            logger.error("Transfer failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Transfer failed"));
        }
    }

    @Operation(summary = "Apply for a loan", description = "Customer applies for a loan (Gold, Home, Education, Vehicle, Personal, Business)")
    @PostMapping("/loan")
    public ResponseEntity<?> applyForLoan(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LoanApplicationRequest loanRequest
    ) {
        if (loanRequest == null ||
            loanRequest.getLoanType() == null ||
            !ALLOWED_LOAN_TYPES.contains(loanRequest.getLoanType()) ||
            loanRequest.getAmountRequested() == null ||
            loanRequest.getAmountRequested().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid loan application. Allowed types: " + ALLOWED_LOAN_TYPES
            ));
        }

        try {
            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));

            Loan loan = Loan.builder()
                    .customer(customer)
                    .loanType(loanRequest.getLoanType())
                    .amountRequested(loanRequest.getAmountRequested())
                    .status("PENDING")
                    .appliedAt(LocalDateTime.now())
                    .reviewedBy(null)
                    .reviewedAt(null)
                    .build();
            loanRepo.save(loan);

            logger.info("Loan application submitted by customer {}: type={}, amount={}", customer.getEmail(), loanRequest.getLoanType(), loanRequest.getAmountRequested());
            return ResponseEntity.ok(Map.of(
                    "message", "Loan application submitted successfully",
                    "loanId", loan.getId()
            ));
        } catch (Exception e) {
            logger.error("Loan application failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Loan application failed"));
        }
    }

    @Operation(summary = "Submit a support ticket", description = "Customer submits a support ticket for 10 predefined banking issues")
    @PostMapping("/ticket")
    public ResponseEntity<?> submitSupportTicket(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody SupportTicketRequest ticketRequest
    ) {
        if (ticketRequest == null ||
            ticketRequest.getIssueType() == null ||
            !ALLOWED_ISSUE_TYPES.contains(ticketRequest.getIssueType())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid issue type. Allowed types: " + ALLOWED_ISSUE_TYPES
            ));
        }

        try {
            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));

            SupportTicket ticket = SupportTicket.builder()
                    .customer(customer)
                    .issueType(ticketRequest.getIssueType())
                    .description(ticketRequest.getDescription())
                    .status("OPEN")
                    .createdAt(LocalDateTime.now())
                    .handledBy(null)
                    .handledAt(null)
                    .build();
            ticketRepo.save(ticket);

            logger.info("Support ticket submitted by customer {}: issueType={}", customer.getEmail(), ticketRequest.getIssueType());
            return ResponseEntity.ok(Map.of(
                    "message", "Support ticket submitted successfully",
                    "ticketId", ticket.getId()
            ));
        } catch (Exception e) {
            logger.error("Support ticket submission failed", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Ticket submission failed"));
        }
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> handleUserNotFound(UsernameNotFoundException ex) {
        logger.error("Customer not found: {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }
}