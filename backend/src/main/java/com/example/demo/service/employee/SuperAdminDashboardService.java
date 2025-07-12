package com.example.demo.service.employee;

import com.example.demo.dto.admin.SuperAdminDashboardDto;
import com.example.demo.repository.customer.CustomerRepository;
import com.example.demo.repository.employee.EmployeeRepository;
import com.example.demo.repository.customer.LoanRepository;
import com.example.demo.repository.customer.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SuperAdminDashboardService {
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final LoanRepository loanRepository;
    private final SupportTicketRepository ticketRepository;

    @Autowired
    public SuperAdminDashboardService(
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            LoanRepository loanRepository,
            SupportTicketRepository ticketRepository
    ) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.loanRepository = loanRepository;
        this.ticketRepository = ticketRepository;
    }

    public SuperAdminDashboardDto getDashboardSummary() {
        SuperAdminDashboardDto dto = new SuperAdminDashboardDto();
        dto.setTotalUsers(customerRepository.count());
        dto.setActiveUsers(customerRepository.countByStatus("ACTIVE"));
        dto.setTotalEmployees(employeeRepository.count());
        dto.setTotalLoans(loanRepository.count());
        dto.setPendingTickets(ticketRepository.countByStatus("PENDING"));

        // Example: sum all loan amounts as total assets (change as needed)
        BigDecimal totalAssets = loanRepository.sumAllLoanAmounts();
        dto.setTotalAssets(totalAssets != null ? totalAssets : BigDecimal.ZERO);

        return dto;
    }
}