package com.example.demo.dto;

public class EmployeeDTO {
    private Long id;
    private String name;
    private String role;
    private String branch; // ✅ New field for branch name

    // Constructors
    public EmployeeDTO() {}

    public EmployeeDTO(Long id, String name, String role, String branch) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.branch = branch;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
