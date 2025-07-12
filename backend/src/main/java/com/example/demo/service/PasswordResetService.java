package com.example.demo.service;

import com.example.demo.model.employee.PasswordResetToken;
import com.example.demo.repository.employee.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final long expiryHours;

    @Autowired
    public PasswordResetService(
            PasswordResetTokenRepository tokenRepo,
            @Value("${reset.token.expiry.hours:1}") long expiryHours
    ) {
        this.tokenRepo = tokenRepo;
        this.expiryHours = expiryHours;
    }

    /**
     * Create and persist a new password-reset token for the given user.
     *
     * @param email      the user’s email
     * @param employeeId the optional employee ID (for employee flows)
     * @return the saved PasswordResetToken
     */
    public PasswordResetToken createToken(String email, String employeeId) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUserEmail(email);
        token.setEmployeeId(employeeId);
        token.setExpiresAt(Instant.now().plus(expiryHours, ChronoUnit.HOURS));
        return tokenRepo.save(token);
    }

    /**
     * Verify that a token exists, has not expired, and matches the given email and employee ID.
     *
     * @param token     the token string
     * @param email     the user’s email
     * @param empId     the employee ID (nullable for customer flows)
     * @return an Optional containing the valid token entity, or empty if invalid/expired
     */
    public Optional<PasswordResetToken> verifyToken(String token, String email, String empId) {
        return tokenRepo.findByToken(token)
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()))
                .filter(t -> t.getUserEmail().equals(email))
                .filter(t -> Objects.equals(t.getEmployeeId(), empId));
    }

    /**
     * Delete a single reset-token by its token string.
     *
     * @param token the token string to delete
     */
    public void deleteToken(String token) {
        tokenRepo.deleteByToken(token);
    }

    /**
     * (Stub) Send a reset link email. Replace System.out with real mail-sender.
     *
     * @param toEmail the recipient’s email
     * @param token   the reset-token to embed in the link
     */
    public void sendResetEmail(String toEmail, String token) {
        String resetUrl = "http://your-domain.com/reset-password.html?token=" + token;
        
        System.out.println("Send email to " + toEmail + " with link: " + resetUrl);
    }
}
