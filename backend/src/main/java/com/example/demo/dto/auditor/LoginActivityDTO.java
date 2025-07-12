package com.example.demo.dto.auditor;

import java.time.LocalDateTime;

public class LoginActivityDTO {
    private Long id;
    private String customerEmail;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String device;

    // No-arg constructor (required for Jackson/Spring)
    public LoginActivityDTO() {}

    // All-args constructor
    public LoginActivityDTO(Long id, String customerEmail, LocalDateTime timestamp, String ipAddress, String device) {
        this.id = id;
        this.customerEmail = customerEmail;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.device = device;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDevice() {
        return device;
    }
    public void setDevice(String device) {
        this.device = device;
    }
}