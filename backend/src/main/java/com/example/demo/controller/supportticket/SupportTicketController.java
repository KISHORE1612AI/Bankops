package com.example.demo.controller.supportticket;

import com.example.demo.dto.customer.SupportTicketRequest;
import com.example.demo.dto.customer.TicketDto;
import com.example.demo.model.customer.SupportTicket;
import com.example.demo.service.customer.SupportTicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/support/tickets")
@PreAuthorize("hasRole('CUSTOMER_SUPPORT')")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @Autowired
    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    // List tickets for dashboard (paginated, optionally filtered by status)
    @GetMapping
    public ResponseEntity<Page<TicketDto>> listTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status
    ) {
        Page<SupportTicket> tickets = supportTicketService.getTicketsForDashboard(status, PageRequest.of(page, size));
        Page<TicketDto> dtoPage = tickets.map(TicketDto::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    // View ticket details
    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketDto> getTicket(@PathVariable Long ticketId) {
        Optional<SupportTicket> ticket = supportTicketService.getTicketById(ticketId);
        return ticket.map(t -> ResponseEntity.ok(TicketDto.fromEntity(t)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Resolve a ticket (mark as RESOLVED) with optional resolution note
    @PutMapping("/{ticketId}/resolve")
    public ResponseEntity<TicketDto> resolveTicket(
            @PathVariable Long ticketId,
            @RequestBody SupportTicketRequest request,
            Principal principal
    ) {
        String handler = principal.getName();
        SupportTicket updated = supportTicketService.resolveTicket(ticketId, handler, request.getResolutionNote());
        return ResponseEntity.ok(TicketDto.fromEntity(updated));
    }

    // Close a ticket (mark as CLOSED) with optional resolution note
    @PutMapping("/{ticketId}/close")
    public ResponseEntity<TicketDto> closeTicket(
            @PathVariable Long ticketId,
            @RequestBody SupportTicketRequest request,
            Principal principal
    ) {
        String handler = principal.getName();
        SupportTicket updated = supportTicketService.closeTicket(ticketId, handler, request.getResolutionNote());
        return ResponseEntity.ok(TicketDto.fromEntity(updated));
    }

    // Reopen a ticket (mark as REOPENED)
    @PutMapping("/{ticketId}/reopen")
    public ResponseEntity<TicketDto> reopenTicket(
            @PathVariable Long ticketId,
            Principal principal
    ) {
        String handler = principal.getName();
        SupportTicket updated = supportTicketService.reopenTicket(ticketId, handler);
        return ResponseEntity.ok(TicketDto.fromEntity(updated));
    }

    // === NEW: Weekly Ticket Stats for Dashboard Chart ===
    @GetMapping("/weekly-stats")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyStats() {
        List<Map<String, Object>> stats = supportTicketService.getWeeklyTicketStats();
        return ResponseEntity.ok(stats);
    }
}