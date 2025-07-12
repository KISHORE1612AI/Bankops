package com.example.demo.controller.employee;

import com.example.demo.dto.admin.SuperAdminEmployeeDto;
import com.example.demo.service.employee.SuperAdminEmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/employees")
public class SuperAdminEmployeeController {

    private final SuperAdminEmployeeService employeeService;

    @Autowired
    public SuperAdminEmployeeController(SuperAdminEmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<SuperAdminEmployeeDto> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    // You can add POST, PUT, DELETE endpoints here for employee management (add/edit/delete)
}