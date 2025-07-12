package com.example.demo.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class Hasher {

    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public static void main(String[] args) {
        printHash("Kishore1612");      // SUPER_ADMIN
        printHash("BMpass@123");       // BRANCH_MANAGER
        printHash("LOpass@123");       // LOAN_OFFICER
        printHash("CSpass@123");       // CUSTOMER_SUPPORT
        printHash("AUDpass@123");      // AUDITOR
    }

    private static void printHash(String password) {
        String hashed = passwordEncoder.encode(password);
        System.out.println("Raw: " + password);
        System.out.println("Hashed: " + hashed);
        System.out.println("-----");
    }
}
