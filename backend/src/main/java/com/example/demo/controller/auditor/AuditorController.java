package com.example.demo.controller.auditor;

import com.example.demo.dto.auditor.AuditorDashboardDTO;
import com.example.demo.dto.auditor.LoginActivityDTO;
import com.example.demo.dto.customer.TransactionDto;
import com.example.demo.service.AuditorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auditor")
@CrossOrigin(origins = "*")
public class AuditorController {

    @Autowired
    private AuditorService auditorService;

    // GET: Dashboard overview summary
    @GetMapping("/dashboard")
    public ResponseEntity<AuditorDashboardDTO> getDashboardOverview() {
        AuditorDashboardDTO dashboardData = auditorService.getDashboardOverview();
        return ResponseEntity.ok(dashboardData);
    }

    // GET: Paginated, filterable login activities
    @GetMapping("/login-activities")
    public ResponseEntity<Page<LoginActivityDTO>> getLoginActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String email
    ) {
        // Defensive: treat empty string as null
        if (date == null || date.trim().isEmpty()) date = null;
        if (email == null || email.trim().isEmpty()) email = null;

        Page<LoginActivityDTO> activities = auditorService.getLoginActivities(page, size, date, email);
        return ResponseEntity.ok(activities);
    }

    // GET: Download login activities as CSV (with same filters)
    @GetMapping("/login-activities/export")
    public ResponseEntity<InputStreamResource> exportLoginActivitiesCSV(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String email
    ) {
        // Defensive: treat empty string as null
        if (date == null || date.trim().isEmpty()) date = null;
        if (email == null || email.trim().isEmpty()) email = null;

        InputStreamResource csv = auditorService.exportLoginActivitiesCSV(date, email);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=login_activities.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // GET: Paginated, filterable transactions
    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionDto>> getTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        // Defensive: treat empty string as null
        if (date == null || date.trim().isEmpty()) date = null;
        if (customer == null || customer.trim().isEmpty()) customer = null;
        if (type == null || type.trim().isEmpty()) type = null;
        if (status == null || status.trim().isEmpty()) status = null;

        Page<TransactionDto> transactions = auditorService.getTransactions(page, size, date, customer, type, status);
        return ResponseEntity.ok(transactions);
    }

    // GET: Download transactions as CSV (with same filters)
    @GetMapping("/transactions/export")
    public ResponseEntity<InputStreamResource> exportTransactionsCSV(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String customer,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status
    ) {
        // Defensive: treat empty string as null
        if (date == null || date.trim().isEmpty()) date = null;
        if (customer == null || customer.trim().isEmpty()) customer = null;
        if (type == null || type.trim().isEmpty()) type = null;
        if (status == null || status.trim().isEmpty()) status = null;

        InputStreamResource csv = auditorService.exportTransactionsCSV(date, customer, type, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // GET: Single login activity details (for modal)
    @GetMapping("/login-activities/{id}")
    public ResponseEntity<LoginActivityDTO> getLoginActivityDetail(@PathVariable Long id) {
        LoginActivityDTO dto = auditorService.getLoginActivityDetail(id);
        return ResponseEntity.ok(dto);
    }

    // GET: Single transaction details (for modal)
    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionDto> getTransactionDetail(@PathVariable Long id) {
        TransactionDto dto = auditorService.getTransactionDetail(id);
        return ResponseEntity.ok(dto);
    }
}