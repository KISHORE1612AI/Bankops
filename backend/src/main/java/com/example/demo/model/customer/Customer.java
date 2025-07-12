package com.example.demo.model.customer;

import com.example.demo.model.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotNull(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    @Column(length = 20)
    private String phone;

    @NotNull(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role;

    @Size(max = 255)
    @Column(length = 255)
    private String address;

    @NotNull(message = "Account number is required")
    @Pattern(regexp = "^[A-Z0-9]{10,15}$", message = "Invalid account number format")
    @Column(name = "account_number", unique = true, nullable = false, length = 15)
    private String accountNumber;

    @Column(name = "account_balance", precision = 19, scale = 4)
    private BigDecimal accountBalance;

    @Column(name = "account_status", length = 50)
    private String accountStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // User status for Super Admin controls: ACTIVE, INACTIVE, etc.
    @Builder.Default
    @Column(name = "status", length = 50)
    private String status = "ACTIVE";

    // Account status enum
    public enum AccountStatus {
        ACTIVE,
        BLOCKED,
        SUSPENDED,
        DORMANT,
        CLOSED
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (accountBalance == null) {
            accountBalance = BigDecimal.ZERO;
        }
        if (accountStatus == null) {
            accountStatus = AccountStatus.ACTIVE.name();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }

    public void updateBalance(BigDecimal amount) {
        if (this.accountBalance == null) {
            this.accountBalance = BigDecimal.ZERO;
        }
        this.accountBalance = this.accountBalance.add(amount);
    }

    public boolean isActive() {
        return AccountStatus.ACTIVE.name().equals(this.accountStatus) && "ACTIVE".equalsIgnoreCase(this.status);
    }

    public boolean hasSufficientBalance(BigDecimal amount) {
        return this.accountBalance != null &&
               this.accountBalance.compareTo(amount) >= 0;
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    public boolean isValid() {
        return fullName != null && !fullName.trim().isEmpty() &&
               email != null && !email.trim().isEmpty() &&
               accountNumber != null && !accountNumber.trim().isEmpty() &&
               accountStatus != null && status != null;
    }

    public void setName(String name) {
        this.fullName = name;
    }

    public String getName() {
        return this.fullName;
    }

    public void activate() {
        this.status = "ACTIVE";
    }

    public void deactivate() {
        this.status = "INACTIVE";
    }

    public String getStatus() {
        return this.status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(role);
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !AccountStatus.CLOSED.name().equals(accountStatus) &&
               !AccountStatus.DORMANT.name().equals(accountStatus);
    }

    @Override
    public boolean isAccountNonLocked() {
        return !AccountStatus.BLOCKED.name().equals(accountStatus) &&
               !AccountStatus.SUSPENDED.name().equals(accountStatus) &&
               !"INACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isActive();
    }

    @Override
    public String toString() {
        return "Customer{" +
               "id=" + id +
               ", fullName='" + fullName + '\'' +
               ", email='" + email + '\'' +
               ", accountNumber='" + accountNumber + '\'' +
               ", accountStatus='" + accountStatus + '\'' +
               ", status='" + status + '\'' +
               '}';
    }
}