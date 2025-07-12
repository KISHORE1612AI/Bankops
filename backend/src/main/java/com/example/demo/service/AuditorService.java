package com.example.demo.service;

import com.example.demo.dto.auditor.AuditorDashboardDTO;
import com.example.demo.dto.auditor.LoginActivityDTO;
import com.example.demo.dto.customer.TransactionDto;
import com.example.demo.model.customer.Customer;
import com.example.demo.model.customer.LoginActivity;
import com.example.demo.model.customer.Transaction;
import com.example.demo.repository.customer.CustomerRepository;
import com.example.demo.repository.customer.LoginActivityRepository;
import com.example.demo.repository.customer.TransactionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditorService {

    @Autowired
    private LoginActivityRepository loginActivityRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    // Dashboard summary data for auditor
    public AuditorDashboardDTO getDashboardOverview() {
        long totalCustomers = customerRepository.count();

        BigDecimal totalBalance = customerRepository.findAll()
                .stream()
                .map(Customer::getAccountBalance)
                .filter(balance -> balance != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalTransactions = transactionRepository.count();

        AuditorDashboardDTO dto = new AuditorDashboardDTO();
        dto.setTotalCustomers(totalCustomers);
        dto.setTotalDeposits(totalBalance);
        dto.setTotalWithdrawals(BigDecimal.ZERO); // Update if withdrawals are tracked separately
        dto.setTotalTransactions(totalTransactions);
        return dto;
    }

    // Paginated/filterable login activities
    public Page<LoginActivityDTO> getLoginActivities(int page, int size, String date, String email) {
        // Defensive: treat empty string as null
        if (date == null || date.trim().isEmpty()) date = null;
        if (email == null || email.trim().isEmpty()) email = null;

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        Page<LoginActivity> activitiesPage;
        if (date == null && email == null) {
            activitiesPage = loginActivityRepository.findAll(pageable);
        } else if (date != null && email == null) {
            LocalDate filterDate = LocalDate.parse(date);
            activitiesPage = loginActivityRepository.findByDate(filterDate, pageable);
        } else if (date == null && email != null) {
            activitiesPage = loginActivityRepository.findByCustomerEmail(email, pageable);
        } else {
            LocalDate filterDate = LocalDate.parse(date);
            activitiesPage = loginActivityRepository.findByDateAndCustomerEmail(filterDate, email, pageable);
        }

        List<LoginActivityDTO> dtoList = activitiesPage.getContent()
                .stream()
                .map(activity -> {
                    Long customerId = activity.getCustomerId();
                    Customer customer = customerRepository.findById(customerId).orElse(null);
                    String customerEmail = customer != null ? customer.getEmail() : "Unknown";

                    return new LoginActivityDTO(
                            activity.getId(),
                            customerEmail,
                            activity.getTimestamp(),
                            activity.getIpAddress(),
                            activity.getDevice()
                    );
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, activitiesPage.getTotalElements());
    }

    // CSV export for login activities
    public InputStreamResource exportLoginActivitiesCSV(String date, String email) {
        // Defensive: treat empty string as null
        if (date == null || date.trim().isEmpty()) date = null;
        if (email == null || email.trim().isEmpty()) email = null;

        Page<LoginActivityDTO> page = getLoginActivities(0, Integer.MAX_VALUE, date, email);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("ID", "Email", "Timestamp", "IP Address", "Device")
                .build();
        try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(out), format)) {
            for (LoginActivityDTO dto : page.getContent()) {
                csvPrinter.printRecord(
                        dto.getId(),
                        dto.getCustomerEmail(),
                        dto.getTimestamp(),
                        dto.getIpAddress(),
                        dto.getDevice()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export login activities CSV", e);
        }
        return new InputStreamResource(new ByteArrayInputStream(out.toByteArray()));
    }

    // Get detail for a single login activity
    public LoginActivityDTO getLoginActivityDetail(Long id) {
        LoginActivity activity = loginActivityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Login activity not found"));
        Customer customer = customerRepository.findById(activity.getCustomerId()).orElse(null);
        String customerEmail = customer != null ? customer.getEmail() : "Unknown";
        return new LoginActivityDTO(
                activity.getId(),
                customerEmail,
                activity.getTimestamp(),
                activity.getIpAddress(),
                activity.getDevice()
        );
    }

    // Paginated/filterable transactions
    public Page<TransactionDto> getTransactions(int page, int size, String date, String customer, String type, String status) {
        // Defensive: treat empty string as null
        if (date == null || date.trim().isEmpty()) date = null;
        if (customer == null || customer.trim().isEmpty()) customer = null;
        if (type == null || type.trim().isEmpty()) type = null;
        if (status == null || status.trim().isEmpty()) status = null;

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        Page<Transaction> txnPage = transactionRepository.advancedSearch(date, customer, type, status, pageable);

        List<TransactionDto> dtoList = txnPage.getContent().stream()
                .map(txn -> new TransactionDto(
                        txn.getId(),
                        txn.getCustomerId(),
                        txn.getRecipientId(),
                        txn.getTimestamp(),
                        txn.getDescription(),
                        txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO,
                        txn.getType(),
                        txn.getStatus()
                ))
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, txnPage.getTotalElements());
    }

    // CSV export for transactions
    public InputStreamResource exportTransactionsCSV(String date, String customer, String type, String status) {
        // Defensive: treat empty string as null
        if (date == null || date.trim().isEmpty()) date = null;
        if (customer == null || customer.trim().isEmpty()) customer = null;
        if (type == null || type.trim().isEmpty()) type = null;
        if (status == null || status.trim().isEmpty()) status = null;

        Page<TransactionDto> page = getTransactions(0, Integer.MAX_VALUE, date, customer, type, status);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("Txn ID", "Customer ID", "Recipient ID", "Timestamp", "Description", "Amount", "Type", "Status")
                .build();
        try (CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(out), format)) {
            for (TransactionDto dto : page.getContent()) {
                csvPrinter.printRecord(
                        dto.getId(),
                        dto.getCustomerId(),
                        dto.getRecipientId(),
                        dto.getTimestamp(),
                        dto.getDescription(),
                        dto.getAmount(),
                        dto.getType(),
                        dto.getStatus()
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export transactions CSV", e);
        }
        return new InputStreamResource(new ByteArrayInputStream(out.toByteArray()));
    }

    // Get detail for a single transaction
    public TransactionDto getTransactionDetail(Long id) {
        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return new TransactionDto(
                txn.getId(),
                txn.getCustomerId(),
                txn.getRecipientId(),
                txn.getTimestamp(),
                txn.getDescription(),
                txn.getAmount() != null ? txn.getAmount() : BigDecimal.ZERO,
                txn.getType(),
                txn.getStatus()
        );
    }
}