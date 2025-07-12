package com.example.demo.controller.employee;

import com.example.demo.model.employee.Employee;
import com.example.demo.repository.employee.EmployeeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/employees")
@Tag(name = "Admin Employee API", description = "Operations for admin management of employees")
public class AdminEmployeeController {

    private static final Logger logger = LoggerFactory.getLogger(AdminEmployeeController.class);

    @Autowired
    private EmployeeRepository employeeRepo;

    @Operation(summary = "Get all employees", description = "Returns a list of all employees for admin/super admin")
    @ApiResponse(responseCode = "200", description = "List of employees retrieved successfully")
    @GetMapping
    public ResponseEntity<?> getAllEmployees() {
        return ResponseEntity.ok(employeeRepo.findAll());
    }

    @Operation(summary = "Get employee by ID", description = "Returns a specific employee by their ID")
    @ApiResponse(responseCode = "200", description = "Employee found")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @GetMapping("/{id}")
    public ResponseEntity<?> getEmployeeById(@PathVariable Long id) {
        Optional<Employee> employee = employeeRepo.findById(id);
        return employee.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete employee by ID", description = "Deletes an employee by their ID")
    @ApiResponse(responseCode = "200", description = "Employee deleted successfully")
    @ApiResponse(responseCode = "404", description = "Employee not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        if (employeeRepo.existsById(id)) {
            employeeRepo.deleteById(id);
            logger.info("Deleted employee with id {}", id);
            return ResponseEntity.ok(Map.of("message", "Employee deleted successfully"));
        } else {
            logger.warn("Attempted to delete non-existent employee with id {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}