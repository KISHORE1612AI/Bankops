package com.example.demo.controller.employee;

import com.example.demo.dto.admin.*;
import com.example.demo.dto.customer.LoanDto;
import com.example.demo.dto.customer.TransactionDto;
import com.example.demo.service.employee.SuperAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin") // <-- NOTE: singular, matches frontend
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @Autowired
    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    // ======= SuperAdmin user/entity management =======
    @GetMapping("/superadmins")
    public List<SuperAdminDto> getAll() {
        return superAdminService.getAll();
    }

    @GetMapping("/superadmins/{id}")
    public ResponseEntity<SuperAdminDto> getById(@PathVariable Long id) {
        return superAdminService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/superadmins")
    public ResponseEntity<SuperAdminDto> create(@Validated @RequestBody SuperAdminCreateRequest req) {
        return ResponseEntity.ok(superAdminService.create(req));
    }

    @PutMapping("/superadmins/{id}")
    public ResponseEntity<SuperAdminDto> update(@PathVariable Long id, @Validated @RequestBody SuperAdminUpdateRequest req) {
        return ResponseEntity.ok(superAdminService.update(id, req));
    }

    @DeleteMapping("/superadmins/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (superAdminService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // ======= LOAN endpoints for Super Admin =======
    @GetMapping("/loans")
    public ResponseEntity<List<LoanDto>> getAllLoans() {
        List<LoanDto> loans = superAdminService.getAllLoans();
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/loans/{id}")
    public ResponseEntity<LoanDto> getLoanById(@PathVariable Long id) {
        return superAdminService.getLoanById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ======= TRANSACTION endpoints for Super Admin =======
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDto>> getAllTransactions() {
        List<TransactionDto> txns = superAdminService.getAllTransactions();
        return ResponseEntity.ok(txns);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable Long id) {
        return superAdminService.getTransactionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}