package com.example.demo.dto.admin;

public class SuperAdminEmployeeDto {
    private Long id;
    private String name;
    private String role;
    private Boolean active;

    public SuperAdminEmployeeDto() {}

    public SuperAdminEmployeeDto(Long id, String name, String role, Boolean active) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.active = active;
    }

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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}