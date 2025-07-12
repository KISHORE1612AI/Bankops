package com.example.demo.repository.customer;

import com.example.demo.model.customer.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Transaction> findFilteredTransactionsForUser(
            Long userId,
            LocalDate fromDate,
            LocalDate toDate,
            String type,
            String status,
            String sortBy,
            String sortOrder
    ) {
        // --- Build JPQL dynamically ---
        StringBuilder sb = new StringBuilder();
        List<String> conds = new ArrayList<>();
        sb.append("SELECT t FROM Transaction t ");
        sb.append("WHERE ((t.customer.id = :userId AND (t.type <> 'TRANSFER_IN' OR t.recipient IS NULL)) ");
        sb.append("OR (t.recipient.id = :userId AND t.type = 'TRANSFER_IN')) ");

        if (fromDate != null) {
            conds.add("t.timestamp >= :fromDateTime");
        }
        if (toDate != null) {
            conds.add("t.timestamp <= :toDateTime");
        }
        if (StringUtils.hasText(type)) {
            conds.add("t.type = :type");
        }
        if (StringUtils.hasText(status)) {
            conds.add("t.status = :status");
        }
        if (!conds.isEmpty()) {
            for (String c : conds) {
                sb.append(" AND ").append(c);
            }
        }

        // --- Sorting ---
        String sortField = "t.timestamp";
        if ("amount".equalsIgnoreCase(sortBy)) sortField = "t.amount";
        String dir = "DESC";
        if ("asc".equalsIgnoreCase(sortOrder)) dir = "ASC";
        sb.append(" ORDER BY ").append(sortField).append(" ").append(dir);

        // --- Create Query ---
        TypedQuery<Transaction> query = entityManager.createQuery(sb.toString(), Transaction.class);
        query.setParameter("userId", userId);
        if (fromDate != null) {
            query.setParameter("fromDateTime", fromDate.atStartOfDay());
        }
        if (toDate != null) {
            query.setParameter("toDateTime", toDate.atTime(23, 59, 59));
        }
        if (StringUtils.hasText(type)) {
            query.setParameter("type", type);
        }
        if (StringUtils.hasText(status)) {
            query.setParameter("status", status);
        }

        // --- Execute and Debug ---
        List<Transaction> result = query.getResultList();
        // DEBUG: Print filter params and result size for troubleshooting export/download issues
        System.out.println("[TransactionRepositoryImpl] User " + userId
                + " filters: fromDate=" + fromDate
                + ", toDate=" + toDate
                + ", type=" + type
                + ", status=" + status
                + ", sortBy=" + sortBy
                + ", sortOrder=" + sortOrder
                + " => transactions found: " + result.size());
        return result;
    }
}