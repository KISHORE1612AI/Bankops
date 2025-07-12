package com.example.demo.controller.customer;

import com.example.demo.model.customer.Customer;
import com.example.demo.repository.customer.CustomerRepository;
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
@RequestMapping("/api/admin/customers")
@Tag(name = "Admin Customer API", description = "Operations for admin management of customers")
public class AdminCustomerController {

    private static final Logger logger = LoggerFactory.getLogger(AdminCustomerController.class);

    @Autowired
    private CustomerRepository customerRepo;

    @Operation(summary = "Get all customers", description = "Returns a list of all customers for admin/super admin")
    @ApiResponse(responseCode = "200", description = "List of customers retrieved successfully")
    @GetMapping
    public ResponseEntity<?> getAllCustomers() {
        return ResponseEntity.ok(customerRepo.findAll());
    }

    @Operation(summary = "Get customer by ID", description = "Returns a specific customer by their ID")
    @ApiResponse(responseCode = "200", description = "Customer found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(@PathVariable Long id) {
        Optional<Customer> customer = customerRepo.findById(id);
        return customer.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete customer by ID", description = "Deletes a customer by their ID")
    @ApiResponse(responseCode = "200", description = "Customer deleted successfully")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        if (customerRepo.existsById(id)) {
            customerRepo.deleteById(id);
            logger.info("Deleted customer with id {}", id);
            return ResponseEntity.ok(Map.of("message", "Customer deleted successfully"));
        } else {
            logger.warn("Attempted to delete non-existent customer with id {}", id);
            return ResponseEntity.notFound().build();
        }
    }
}