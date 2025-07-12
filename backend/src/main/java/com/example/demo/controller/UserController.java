package com.example.demo.controller;

import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.UserRequest;
import com.example.demo.model.Role;
import com.example.demo.service.UserService;
import com.example.demo.service.JwtService;
import com.example.demo.service.UnifiedUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = "http://localhost:8000")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UnifiedUserDetailsService userDetailsService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    // ✅ Get all users or filter by role
    @GetMapping
    public ResponseEntity<?> getUsers(@RequestParam(required = false) Role role) {
        if (role == null) {
            return ResponseEntity.ok(userService.getAllUsers());
        } else {
            return ResponseEntity.ok(userService.getUsersByRole(role));
        }
    }

    // ✅ Create user with any role
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserRequest userRequest) {
        if (userService.userExists(userRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body("User with this email already exists.");
        }

        Object created = userService.createUser(userRequest);
        return ResponseEntity.ok(created);
    }

    // ✅ Update user by ID
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserRequest userRequest) {
        Object updated = userService.updateUser(id, userRequest);
        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ Delete user by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.ok("User deleted successfully.");
        } else {
            return ResponseEntity.status(404).body("User not found.");
        }
    }

    // ✅ Customer and employee login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String email = loginData.get("email");
        String password = loginData.get("password");

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            String token = jwtService.generateToken(userDetails);

            return ResponseEntity.ok(Map.of("token", token));
        } catch (Exception e) {
            return ResponseEntity
                    .status(401)
                    .body("Invalid email or password.");
        }
    }

    // ✅ Change password (Customer or Employee)
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        boolean result = userService.changePassword(request.getEmail(), request.getCurrentPassword(), request.getNewPassword());
        if (result) {
            return ResponseEntity.ok("Password changed successfully.");
        } else {
            return ResponseEntity.status(400).body("Invalid email or current password.");
        }
    }
}