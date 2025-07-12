package com.example.demo.service.employee;

import com.example.demo.dto.admin.SupportTicketDto;
import java.util.List;

public interface SuperAdminTicketService {
    List<SupportTicketDto> getAllTickets();
    // Optionally add: SupportTicketDto getTicketById(Long id), void resolveTicket(Long id), etc.
}