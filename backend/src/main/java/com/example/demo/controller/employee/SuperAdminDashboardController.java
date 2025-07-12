package com.example.demo.controller.employee;

import com.example.demo.dto.admin.SuperAdminDashboardDto;
import com.example.demo.service.employee.SuperAdminDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/superadmin/dashboard")
public class SuperAdminDashboardController {

    private final SuperAdminDashboardService dashboardService;

    @Autowired
    public SuperAdminDashboardController(SuperAdminDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<SuperAdminDashboardDto> getDashboardSummary() {
        return ResponseEntity.ok(dashboardService.getDashboardSummary());
    }
}