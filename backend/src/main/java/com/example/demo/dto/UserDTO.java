package com.example.demo.dto;

public class UserDTO {
    private Long id;
    private String username;     // for login/username use-case (optional)
    private String fullName;     // for dashboard/display name
    private String email;
    private String phone;
    private String accountNumber;
    private String accountStatus;
    private String address;

    // Constructors
    public UserDTO() {}

    public UserDTO(Long id, String username, String fullName, String email, String phone, String accountNumber, String accountStatus, String address) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.accountNumber = accountNumber;
        this.accountStatus = accountStatus;
        this.address = address;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}