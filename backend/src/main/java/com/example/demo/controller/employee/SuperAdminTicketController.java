package com.example.demo.controller.employee;

import com.example.demo.dto.admin.SupportTicketDto;
import com.example.demo.service.employee.SuperAdminTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/tickets")
public class SuperAdminTicketController {

    private final SuperAdminTicketService ticketService;

    @Autowired
    public SuperAdminTicketController(SuperAdminTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<SupportTicketDto> getAllTickets() {
        return ticketService.getAllTickets();
    }

    // You can add POST, PUT, PATCH endpoints here for ticket management if needed
}