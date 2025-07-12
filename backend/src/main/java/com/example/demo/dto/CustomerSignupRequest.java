package com.example.demo.dto;

/**
 * DTO for customer signup requests.
 * Keeps input clean and avoids exposing sensitive fields.
 */
public class CustomerSignupRequest {

    private String fullName;
    private String email;
    private String password;
    private String phone; // Optional

    public CustomerSignupRequest() { }

    public String getFullName() {
        return fullName;
    }
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }
}
