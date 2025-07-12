package com.example.demo.dto;

public class ForgotPasswordRequest {
    private String email;
    private String employeeId; // Optional for employees only

    // Getters & Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
}
