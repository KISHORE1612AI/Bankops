package com.example.demo.repository.customer;

import com.example.demo.model.customer.Transaction;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepositoryCustom {
    List<Transaction> findFilteredTransactionsForUser(
        Long userId,
        LocalDate fromDate,
        LocalDate toDate,
        String type,
        String status,
        String sortBy,
        String sortOrder
    );
}