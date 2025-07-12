package com.example.demo.controller;

import com.example.demo.dto.CustomerSignupRequest;
import com.example.demo.dto.ForgotPasswordRequest;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.ResetPasswordRequest;
import com.example.demo.model.Role;
import com.example.demo.model.customer.Customer;
import com.example.demo.model.customer.LoginActivity;
import com.example.demo.model.employee.Employee;
import com.example.demo.model.employee.PasswordResetToken;
import com.example.demo.model.employee.RefreshToken;
import com.example.demo.repository.customer.CustomerRepository;
import com.example.demo.repository.customer.LoginActivityRepository;
import com.example.demo.repository.employee.EmployeeRepository;
import com.example.demo.service.JwtService;
import com.example.demo.service.PasswordResetService;
import com.example.demo.service.RefreshTokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private CustomerRepository customerRepo;
    @Autowired private EmployeeRepository employeeRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordResetService resetService;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private LoginActivityRepository loginActivityRepo;

    // 🔐 Generate unique 12-digit account number
    private String generateUniqueAccountNumber() {
        String number;
        do {
            number = String.valueOf(100000000000L + (long)(Math.random() * 900000000000L));
        } while (customerRepo.existsByAccountNumber(number));
        return number;
    }

    // --- CUSTOMER SIGNUP ---
    @PostMapping("/customer/signup")
    public ResponseEntity<?> signupCustomer(@RequestBody CustomerSignupRequest request) {
        if (customerRepo.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        Customer newCustomer = new Customer();
        newCustomer.setEmail(request.getEmail());
        newCustomer.setFullName(request.getFullName());
        newCustomer.setPhone(request.getPhone());
        newCustomer.setPassword(passwordEncoder.encode(request.getPassword()));
        newCustomer.setRole(Role.CUSTOMER);
        newCustomer.setAccountNumber(generateUniqueAccountNumber());
        newCustomer.setAccountBalance(BigDecimal.ZERO);
        newCustomer.setAccountStatus("ACTIVE");

        customerRepo.save(newCustomer);
        return ResponseEntity.ok("Customer registered successfully");
    }

    // --- CUSTOMER LOGIN ---
    @PostMapping("/customer/login")
    public ResponseEntity<?> loginCustomer(@RequestBody LoginRequest req, HttpServletRequest request) {
        Optional<Customer> oc = customerRepo.findByEmail(req.getEmail())
            .filter(c -> passwordEncoder.matches(req.getPassword(), c.getPassword()));
        if (oc.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        Customer c = oc.get();

        // ---- UPDATE lastLogin here ----
        c.setLastLogin(LocalDateTime.now());
        customerRepo.save(c);

        String accessToken = jwtService.generateToken(c);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(c.getEmail());

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        LoginActivity loginActivity = new LoginActivity(
            c.getId(),
            LocalDateTime.now(),
            ip,
            userAgent
        );
        loginActivityRepo.save(loginActivity);

        return ResponseEntity.ok(Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken.getToken(),
            "role", c.getRole().name()
        ));
    }

    // --- EMPLOYEE LOGIN ---
    @PostMapping("/employee/login")
    public ResponseEntity<?> loginEmployee(@RequestBody LoginRequest req) {
        Optional<Employee> opt = employeeRepo.findByEmail(req.getEmail());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email");
        }

        Employee e = opt.get();

        if (!e.getEmpId().equals(req.getEmployeeId())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid employee ID");
        }

        if (!passwordEncoder.matches(req.getPassword(), e.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password");
        }

        String accessToken = jwtService.generateToken(e);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(e.getEmail());

        return ResponseEntity.ok(Map.of(
            "accessToken", accessToken,
            "refreshToken", refreshToken.getToken(),
            "role", e.getRole().name()
        ));
    }

    // --- REFRESH ACCESS TOKEN ---
    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshAccessToken(@RequestBody Map<String, String> body) {
        String token = body.get("refreshToken");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Refresh token required");
        }

        Optional<RefreshToken> optional = refreshTokenService.verifyRefreshToken(token);
        if (optional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh limit exceeded or session expired");
        }

        RefreshToken rt = optional.get();
        long now = System.currentTimeMillis(),
             lastUsed = rt.getLastUsedAt().toEpochMilli(),
             idleLimitMillis = 2 * 60 * 1000;

        if ((now - lastUsed) > idleLimitMillis) {
            refreshTokenService.deleteByToken(token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Session timed out due to inactivity. Please log in again.");
        }

        if (rt.getIterationCount() >= 2) {
            refreshTokenService.deleteByToken(token);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Refresh limit exceeded. Please log in again.");
        }

        rt.setIterationCount(rt.getIterationCount() + 1);
        rt.setLastUsedAt(Instant.now());
        refreshTokenService.save(rt);

        Optional<Customer> cust = customerRepo.findByEmail(rt.getUserEmail());
        Optional<Employee> emp = employeeRepo.findByEmail(rt.getUserEmail());

        if (cust.isPresent()) {
            String newAccessToken = jwtService.generateToken(cust.get());
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } else if (emp.isPresent()) {
            String newAccessToken = jwtService.generateToken(emp.get());
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found for this token");
    }

    // --- FORGOT PASSWORD ---
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        Optional<Customer> cust = customerRepo.findByEmail(req.getEmail());
        Optional<Employee> emp = employeeRepo.findByEmail(req.getEmail());

        if (cust.isEmpty() && emp.isEmpty()) {
            return ResponseEntity.ok("If that email is registered, a reset link has been sent.");
        }

        PasswordResetToken token = resetService.createToken(req.getEmail(), req.getEmployeeId());
        resetService.sendResetEmail(req.getEmail(), token.getToken());

        return ResponseEntity.ok("If that email is registered, a reset link has been sent.");
    }

    // --- RESET PASSWORD ---
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        PasswordResetToken prt = resetService.verifyToken(
            req.getToken(), req.getEmail(), req.getEmployeeId()
        ).orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        customerRepo.findByEmail(req.getEmail()).ifPresent(c -> {
            c.setPassword(passwordEncoder.encode(req.getNewPassword()));
            customerRepo.save(c);
        });

        employeeRepo.findByEmail(req.getEmail()).ifPresent(emp -> {
            emp.setPassword(passwordEncoder.encode(req.getNewPassword()));
            employeeRepo.save(emp);
        });

        resetService.deleteToken(prt.getToken());
        return ResponseEntity.ok("Password updated successfully.");
    }
}