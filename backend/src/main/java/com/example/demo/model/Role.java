package com.example.demo.model;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    SUPER_ADMIN,
    BRANCH_MANAGER,
    LOAN_OFFICER,
    CUSTOMER_SUPPORT,
    AUDITOR,
    CUSTOMER;

    @Override
    public String getAuthority() {
        return name();
    }
}
