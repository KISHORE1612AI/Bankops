package com.example.demo.service;

import com.example.demo.dto.UserRequest;
import com.example.demo.model.Role;
import com.example.demo.model.customer.Customer;
import com.example.demo.model.customer.Transaction;
import com.example.demo.model.employee.Employee;
import com.example.demo.repository.customer.CustomerRepository;
import com.example.demo.repository.customer.TransactionRepository;
import com.example.demo.repository.employee.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(
            CustomerRepository customerRepository,
            EmployeeRepository employeeRepository,
            TransactionRepository transactionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public boolean userExists(String email) {
        return customerRepository.findByEmail(email).isPresent()
                || employeeRepository.findByEmail(email).isPresent();
    }

    public Customer addCustomer(Customer customer) {
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));
        customer.setRole(Role.CUSTOMER);

        // Set account creation defaults if not already set
        if (customer.getAccountNumber() == null || customer.getAccountNumber().isEmpty()) {
            customer.setAccountNumber(generateUniqueAccountNumber());
        }
        if (customer.getAccountBalance() == null) {
            customer.setAccountBalance(BigDecimal.ZERO);
        }
        if (customer.getAccountStatus() == null || customer.getAccountStatus().isEmpty()) {
            customer.setAccountStatus("ACTIVE");
        }

        // Set status for Super Admin activation/deactivation (default ACTIVE)
        if (customer.getStatus() == null || customer.getStatus().isEmpty()) {
            customer.setStatus("ACTIVE");
        }

        // Branch logic removed

        return customerRepository.save(customer);
    }

    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Customer not found with email: " + email));
    }

    public boolean changePassword(String email, String currentPassword, String newPassword) {
        var custOpt = customerRepository.findByEmail(email);
        if (custOpt.isPresent()) {
            Customer c = custOpt.get();
            if (passwordEncoder.matches(currentPassword, c.getPassword())) {
                c.setPassword(passwordEncoder.encode(newPassword));
                customerRepository.save(c);
                return true;
            }
            return false;
        }

        var empOpt = employeeRepository.findByEmail(email);
        if (empOpt.isPresent()) {
            Employee e = empOpt.get();
            if (passwordEncoder.matches(currentPassword, e.getPassword())) {
                e.setPassword(passwordEncoder.encode(newPassword));
                employeeRepository.save(e);
                return true;
            }
            return false;
        }

        return false;
    }

    public Object createUser(UserRequest request) {
        if (userExists(request.getEmail())) {
            throw new RuntimeException("User with this email already exists");
        }

        if (request.getRole() == Role.CUSTOMER) {
            Customer customer = new Customer();
            customer.setFullName(request.getName());
            customer.setEmail(request.getEmail());
            customer.setPassword(passwordEncoder.encode(request.getPassword()));
            customer.setRole(Role.CUSTOMER);
            customer.setPhone(request.getPhone());
            customer.setAddress(request.getAddress());

            customer.setAccountNumber(generateUniqueAccountNumber());
            customer.setAccountBalance(BigDecimal.ZERO);
            customer.setAccountStatus("ACTIVE");

            // Super Admin: set customer status (enable/disable by admin if needed)
            customer.setStatus(request.getStatus() == null ? "ACTIVE" : request.getStatus());

            // Branch logic removed

            return customerRepository.save(customer);
        } else {
            Employee employee = new Employee();
            employee.setName(request.getName());
            employee.setEmail(request.getEmail());
            employee.setPassword(passwordEncoder.encode(request.getPassword()));
            employee.setRole(request.getRole());
            employee.setPhone(request.getPhone());
            employee.setAddress(request.getAddress());

            // Branch logic removed

            return employeeRepository.save(employee);
        }
    }

    public List<Object> getAllUsers() {
        List<Object> allUsers = new ArrayList<>();
        allUsers.addAll(customerRepository.findAll());
        allUsers.addAll(employeeRepository.findAll());
        return allUsers;
    }

    public List<?> getUsersByRole(Role role) {
        if (role == Role.CUSTOMER) {
            return customerRepository.findAll();
        } else {
            return employeeRepository.findByRole(role);
        }
    }

    public Optional<Customer> fetchCustomerProfile(String email) {
        return customerRepository.findByEmail(email);
    }

    public List<Customer> getCustomersByStatus(String status) {
        // Super Admin: filter using status field
        return customerRepository.findAllByStatus(status);
    }

    public Optional<Customer> getCustomerByAccountNumber(String accountNumber) {
        return customerRepository.findByAccountNumber(accountNumber);
    }

    public Object updateUser(Long id, UserRequest request) {
        if (request.getRole() == Role.CUSTOMER) {
            Optional<Customer> customerOpt = customerRepository.findById(id);
            if (customerOpt.isPresent()) {
                Customer c = customerOpt.get();
                c.setFullName(request.getName());
                c.setEmail(request.getEmail());
                if (request.getPassword() != null) {
                    c.setPassword(passwordEncoder.encode(request.getPassword()));
                }
                c.setPhone(request.getPhone());
                c.setAddress(request.getAddress());
                // Super Admin: update customer status if provided
                if (request.getStatus() != null) {
                    c.setStatus(request.getStatus());
                }
                // Branch logic removed
                return customerRepository.save(c);
            }
        } else {
            Optional<Employee> employeeOpt = employeeRepository.findById(id);
            if (employeeOpt.isPresent()) {
                Employee e = employeeOpt.get();
                e.setName(request.getName());
                e.setEmail(request.getEmail());
                if (request.getPassword() != null) {
                    e.setPassword(passwordEncoder.encode(request.getPassword()));
                }
                e.setRole(request.getRole());
                e.setPhone(request.getPhone());
                e.setAddress(request.getAddress());
                // Branch logic removed
                return employeeRepository.save(e);
            }
        }

        throw new RuntimeException("User not found for update");
    }

    public boolean deleteUser(Long id) {
        if (customerRepository.existsById(id)) {
            customerRepository.deleteById(id);
            return true;
        } else if (employeeRepository.existsById(id)) {
            employeeRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private String generateUniqueAccountNumber() {
        String number;
        do {
            number = String.valueOf(100000000000L + (long)(Math.random() * 900000000000L));
        } while (customerRepository.existsByAccountNumber(number));
        return number;
    }

    @Transactional
    public String transferFunds(String senderEmail, String recipientAccountNumber, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        Customer sender = customerRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        if (sender.getAccountNumber() == null || sender.getAccountNumber().equals(recipientAccountNumber)) {
            throw new IllegalArgumentException("Invalid recipient account");
        }

        Customer recipient = customerRepository.findByAccountNumber(recipientAccountNumber)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        if (sender.getAccountBalance() == null || sender.getAccountBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        sender.setAccountBalance(sender.getAccountBalance().subtract(amount));
        recipient.setAccountBalance(
                recipient.getAccountBalance() == null ? amount : recipient.getAccountBalance().add(amount)
        );
        customerRepository.save(sender);
        customerRepository.save(recipient);

        Transaction senderTx = Transaction.builder()
                .customer(sender)
                .recipient(recipient)
                .amount(amount)
                .type("TRANSFER_OUT")
                .status("SUCCESS")
                .description("Transferred to account: " + recipient.getAccountNumber())
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(senderTx);

        Transaction recipientTx = Transaction.builder()
                .customer(recipient)
                .recipient(sender)
                .amount(amount)
                .type("TRANSFER_IN")
                .status("SUCCESS")
                .description("Received from account: " + sender.getAccountNumber())
                .timestamp(LocalDateTime.now())
                .build();
        transactionRepository.save(recipientTx);

        return "Transfer successful";
    }
}