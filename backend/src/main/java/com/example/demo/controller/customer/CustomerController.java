package com.example.demo.controller.customer;

import com.example.demo.model.customer.Customer;
import com.example.demo.repository.customer.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customer")
@Tag(name = "Customer API", description = "Operations pertaining to customer management")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @Autowired
    private CustomerRepository customerRepo;

    @Operation(summary = "Get customer profile", description = "Returns the profile information of the authenticated customer")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved customer profile")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping("/profile")
    public ResponseEntity<?> getCustomerProfile(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found for email: " + userDetails.getUsername()));

            Map<String, Object> payload = new HashMap<>();
            payload.put("fullName", customer.getFullName());
            payload.put("email", customer.getEmail());
            payload.put("phone", customer.getPhone() != null ? customer.getPhone() : "");
            payload.put("accountNumber", customer.getAccountNumber() != null ? customer.getAccountNumber() : "");
            payload.put("accountBalance", customer.getAccountBalance() != null ? customer.getAccountBalance() : BigDecimal.ZERO);
            payload.put("accountStatus", customer.getAccountStatus() != null ? customer.getAccountStatus() : "ACTIVE");
            payload.put("address", customer.getAddress() != null ? customer.getAddress() : "");

            logger.debug("Retrieved profile for customer: {}", customer.getEmail());
            return ResponseEntity.ok(payload);
        } catch (UsernameNotFoundException e) {
            logger.error("Customer profile not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error retrieving customer profile: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @Operation(summary = "Update customer profile", description = "Updates the profile information of the authenticated customer")
    @ApiResponse(responseCode = "200", description = "Successfully updated customer profile")
    @ApiResponse(responseCode = "400", description = "Invalid input data")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @PutMapping("/profile")
    public ResponseEntity<?> updateCustomerProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody Map<String, String> updates) {
        try {
            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));

            // Update only allowed fields
            if (updates.containsKey("phone")) {
                customer.setPhone(updates.get("phone"));
            }
            if (updates.containsKey("address")) {
                customer.setAddress(updates.get("address"));
            }

            customerRepo.save(customer);
            logger.debug("Updated profile for customer: {}", customer.getEmail());

            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
        } catch (UsernameNotFoundException e) {
            logger.error("Customer not found for profile update: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating customer profile: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @Operation(summary = "Get account balance", description = "Returns the current balance of the authenticated customer")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved account balance")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping("/balance")
    public ResponseEntity<?> getAccountBalance(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            Customer customer = customerRepo.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Customer not found"));

            return ResponseEntity.ok(Map.of(
                "accountNumber", customer.getAccountNumber(),
                "balance", customer.getAccountBalance() != null ? customer.getAccountBalance() : BigDecimal.ZERO,
                "status", customer.getAccountStatus()
            ));
        } catch (UsernameNotFoundException e) {
            logger.error("Customer not found for balance check: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error retrieving account balance: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "An unexpected error occurred"));
        }
    }

    @Operation(
        summary = "Verify customer by account number and/or name",
        description = "Checks if a customer exists with the given account number and (optional) name"
    )
    @ApiResponse(responseCode = "200", description = "Customer found")
    @ApiResponse(responseCode = "404", description = "Customer not found")
    @GetMapping("/verify")
    public ResponseEntity<?> verifyCustomer(
            @RequestParam String accountNumber,
            @RequestParam(required = false) String fullName
    ) {
        Optional<Customer> customerOpt = customerRepo.findByAccountNumber(accountNumber);
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            if (fullName != null && !fullName.trim().isEmpty()) {
                if (!customer.getFullName().equalsIgnoreCase(fullName.trim())) {
                    logger.warn("Customer name does not match for account {}: expected {}, got {}", accountNumber, customer.getFullName(), fullName);
                    return ResponseEntity.status(400).body(Map.of("error", "Account number and name do not match"));
                }
            }
            return ResponseEntity.ok(Map.of(
                "accountNumber", customer.getAccountNumber(),
                "fullName", customer.getFullName(),
                "email", customer.getEmail()
            ));
        } else {
            logger.warn("Customer not found for verification: {}", accountNumber);
            return ResponseEntity.status(404).body(Map.of("error", "Customer not found for verification"));
        }
    }

    // Exception handler for validation errors
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<?> handleValidationExceptions(jakarta.validation.ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        });
        return ResponseEntity.badRequest().body(errors);
    }
}