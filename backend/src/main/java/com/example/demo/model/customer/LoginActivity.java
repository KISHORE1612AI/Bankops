package com.example.demo.model.customer;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_login_activity") // renamed to avoid conflicts
public class LoginActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "device")
    private String device;

    // No-arg constructor required by JPA
    public LoginActivity() {}

    // All-args constructor
    public LoginActivity(Long customerId, LocalDateTime timestamp, String ipAddress, String device) {
        this.customerId = customerId;
        this.timestamp = timestamp;
        this.ipAddress = ipAddress;
        this.device = device;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getDevice() {
        return device;
    }

    // Setters
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}
