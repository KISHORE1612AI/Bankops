package com.example.demo.dto;

import com.example.demo.model.Role;

public class UserRequest {
    private String name;
    private String email;
    private String password;
    private Role role;

    private String accountType; // For customers, if needed
    private String phone;
    private String address;

    // For customer/employee status (ACTIVE, INACTIVE, etc.)
    private String status;

    // === Getters and Setters ===
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}