package com.example.demo.service;

import com.example.demo.repository.customer.CustomerRepository;
import com.example.demo.repository.employee.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * UnifiedUserDetailsService allows authentication for both employees and customers
 * using the same UserDetailsService interface for Spring Security.
 */
@Service
public class UnifiedUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedUserDetailsService.class);

    @Autowired
    private CustomerRepository customerRepo;

    @Autowired
    private EmployeeRepository employeeRepo;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // First check in employee repository
        return employeeRepo.findByEmail(email)
            .map(e -> {
                logger.info("Loaded employee user: {}", email);
                return (UserDetails) e;
            })
            // Then check in customer repository
            .or(() -> customerRepo.findByEmail(email).map(c -> {
                logger.info("Loaded customer user: {}", email);
                return (UserDetails) c;
            }))
            .orElseThrow(() -> {
                logger.warn("User not found: {}", email);
                return new UsernameNotFoundException("User not found with email: " + email);
            });
    }
}