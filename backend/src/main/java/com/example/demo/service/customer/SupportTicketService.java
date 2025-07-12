package com.example.demo.service.customer;

import com.example.demo.model.customer.SupportTicket;
import com.example.demo.model.customer.Customer;
import com.example.demo.repository.customer.SupportTicketRepository;
import com.example.demo.repository.customer.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final CustomerRepository customerRepository;

    @Autowired
    public SupportTicketService(SupportTicketRepository supportTicketRepository, CustomerRepository customerRepository) {
        this.supportTicketRepository = supportTicketRepository;
        this.customerRepository = customerRepository;
    }

    // Create a new support ticket for a customer
    @Transactional
    public SupportTicket createTicket(Long customerId, SupportTicket ticket) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        ticket.setCustomer(customer);
        ticket.setStatus(SupportTicket.TicketStatus.OPEN.name());
        ticket.setCreatedAt(LocalDateTime.now());
        return supportTicketRepository.save(ticket);
    }

    // Get all tickets for a specific customer
    public List<SupportTicket> getTicketsForCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        return supportTicketRepository.findByCustomer(customer);
    }

    // Get paginated tickets for dashboard, by status (or all if status is null)
    public Page<SupportTicket> getTicketsForDashboard(String status, Pageable pageable) {
        if (status == null || status.isBlank()) {
            return supportTicketRepository.findAll(pageable);
        }
        return supportTicketRepository.findByStatus(status, pageable);
    }

    // Get all tickets by status (for support staff dashboard, non-paginated)
    public List<SupportTicket> getTicketsByStatus(String status) {
        return supportTicketRepository.findByStatus(status);
    }

    // Get a single ticket by ID
    public Optional<SupportTicket> getTicketById(Long ticketId) {
        return supportTicketRepository.findById(ticketId);
    }

    // --- Paginated query with filtering for support dashboard ---
    public Page<SupportTicket> getTicketsForSupportDashboard(
            String status, String issueType, String handledBy, Pageable pageable) {
        if (status != null && issueType != null && handledBy != null) {
            return supportTicketRepository.findByStatusAndIssueType(status, issueType, pageable);
        } else if (status != null && handledBy != null) {
            return supportTicketRepository.findByStatusAndHandledBy(status, handledBy, pageable);
        } else if (status != null && issueType != null) {
            return supportTicketRepository.findByIssueTypeAndStatus(issueType, status, pageable);
        } else if (status != null) {
            return supportTicketRepository.findByStatus(status, pageable);
        }
        return supportTicketRepository.findAll(pageable);
    }

    // Update/handle/resolve a ticket with resolution note
    @Transactional
    public SupportTicket handleAndResolveTicket(
            Long ticketId, String handledBy, 
            SupportTicket.TicketStatus newStatus, String resolutionNote) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found"));
        ticket.setHandledBy(handledBy);
        ticket.setHandledAt(LocalDateTime.now());
        ticket.updateStatus(newStatus, resolutionNote);
        return supportTicketRepository.save(ticket);
    }

    // Mark a ticket as resolved with a resolution note
    @Transactional
    public SupportTicket resolveTicket(Long ticketId, String handledBy, String resolutionNote) {
        return handleAndResolveTicket(ticketId, handledBy, SupportTicket.TicketStatus.RESOLVED, resolutionNote);
    }

    // Mark a ticket as closed with a resolution note
    @Transactional
    public SupportTicket closeTicket(Long ticketId, String handledBy, String resolutionNote) {
        return handleAndResolveTicket(ticketId, handledBy, SupportTicket.TicketStatus.CLOSED, resolutionNote);
    }

    // Reopen a ticket
    @Transactional
    public SupportTicket reopenTicket(Long ticketId, String handledBy) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found"));
        ticket.setHandledBy(handledBy);
        ticket.setHandledAt(LocalDateTime.now());
        ticket.reopen();
        return supportTicketRepository.save(ticket);
    }

    // Weekly ticket stats for dashboard chart
    public List<Map<String, Object>> getWeeklyTicketStats() {
        // Get last 7 days (including today)
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);

        // Query DB for counts per date
        List<Object[]> dbStats = supportTicketRepository.countTicketsPerDate(java.sql.Date.valueOf(start));

        // Prepare map of date -> count, fill with zeros first
        Map<LocalDate, Integer> dateCountMap = new LinkedHashMap<>();
        for (long i = 0; i < 7; i++) {
            dateCountMap.put(start.plusDays(i), 0);
        }
        for (Object[] row : dbStats) {
            LocalDate date;
            Object dateObj = row[0];
            if (dateObj instanceof java.sql.Date) {
                date = ((java.sql.Date) dateObj).toLocalDate();
            } else if (dateObj instanceof LocalDate) {
                date = (LocalDate) dateObj;
            } else if (dateObj instanceof java.time.LocalDateTime) {
                date = ((java.time.LocalDateTime) dateObj).toLocalDate();
            } else if (dateObj instanceof String) {
                date = LocalDate.parse((String) dateObj);
            } else {
                throw new IllegalArgumentException("Cannot parse date from DB result: " + dateObj);
            }
            Number count = (Number) row[1];
            dateCountMap.put(date, count.intValue());
        }
        // Transform to desired List<Map<String,Object>> format
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<LocalDate, Integer> entry : dateCountMap.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("day", entry.getKey().toString());
            m.put("count", entry.getValue());
            result.add(m);
        }
        return result;
    }

    // SUPER ADMIN: Assignment/reassignment features

    // Assign a ticket to an employee (by empId)
    @Transactional
    public SupportTicket assignTicketToEmployee(Long ticketId, String empId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Support ticket not found"));
        ticket.assignTo(empId);
        return supportTicketRepository.save(ticket);
    }

    // Get all tickets assigned to a specific employee (non-paginated)
    public List<SupportTicket> getTicketsAssignedToEmpId(String empId) {
        return supportTicketRepository.findByAssignedToEmpId(empId);
    }

    // Get paginated tickets assigned to a specific employee
    public Page<SupportTicket> getTicketsAssignedToEmpId(String empId, Pageable pageable) {
        return supportTicketRepository.findByAssignedToEmpId(empId, pageable);
    }

    // Get all tickets by status and assigned employee (non-paginated)
    public List<SupportTicket> getTicketsByStatusAndAssignedToEmpId(String status, String empId) {
        return supportTicketRepository.findByStatusAndAssignedToEmpId(status, empId);
    }

    // Get paginated tickets by status and assigned employee
    public Page<SupportTicket> getTicketsByStatusAndAssignedToEmpId(String status, String empId, Pageable pageable) {
        return supportTicketRepository.findByStatusAndAssignedToEmpId(status, empId, pageable);
    }

    // === DASHBOARD: Count tickets by status (for Super Admin dashboard) ===
    public long countTicketsByStatus(String status) {
        return supportTicketRepository.countByStatus(status);
    }
}