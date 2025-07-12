package com.example.demo.service.export;

import com.example.demo.model.customer.Customer;
import com.example.demo.model.customer.Transaction;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface TransactionExportService {
    ByteArrayInputStream transactionsToCsv(List<Transaction> transactions);

    ByteArrayInputStream transactionsToPdf(List<Transaction> transactions, Customer customer);
}