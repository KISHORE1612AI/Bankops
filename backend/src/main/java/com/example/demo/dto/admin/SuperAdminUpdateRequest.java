package com.example.demo.dto.admin;

import jakarta.validation.constraints.*;

public class SuperAdminUpdateRequest {
    private String name;
    private String phone;
    @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status must be ACTIVE or INACTIVE")
    private String status;

    // Getters and setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}