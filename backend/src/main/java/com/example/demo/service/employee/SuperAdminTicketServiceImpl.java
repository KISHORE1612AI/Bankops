package com.example.demo.service.employee;

import com.example.demo.dto.admin.SupportTicketDto;
import com.example.demo.model.customer.SupportTicket;
import com.example.demo.repository.customer.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuperAdminTicketServiceImpl implements SuperAdminTicketService {

    private final SupportTicketRepository supportTicketRepository;

    @Autowired
    public SuperAdminTicketServiceImpl(SupportTicketRepository supportTicketRepository) {
        this.supportTicketRepository = supportTicketRepository;
    }

    @Override
    public List<SupportTicketDto> getAllTickets() {
        List<SupportTicket> tickets = supportTicketRepository.findAll();
        // Map SupportTicket -> SupportTicketDto
        return tickets.stream()
                .map(t -> new SupportTicketDto(
                        t.getId(),
                        t.getCustomer() != null ? t.getCustomer().getName() : null, // Adjust if you need customer name
                        t.getIssueType(),
                        t.getStatus(),
                        t.getCreatedAt(),
                        t.getHandledBy()
                ))
                .collect(Collectors.toList());
    }
}