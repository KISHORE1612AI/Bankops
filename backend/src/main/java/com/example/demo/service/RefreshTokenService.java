package com.example.demo.service;

import com.example.demo.model.employee.RefreshToken;
import com.example.demo.repository.employee.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final long REFRESH_TOKEN_VALIDITY_MINUTES = 30;
    private static final long IDLE_EXPIRY_AFTER_ACCESS_MINUTES = 2;
    private static final int MAX_ITERATION_COUNT = 2;

    @Autowired
    private RefreshTokenRepository refreshTokenRepo;

    // Create a new refresh token for a user
    public RefreshToken createRefreshToken(String userEmail) {
        RefreshToken token = new RefreshToken();
        token.setUserEmail(userEmail);
        token.setToken(UUID.randomUUID().toString());
        token.setIssuedAt(Instant.now());
        token.setLastUsedAt(Instant.now());
        token.setExpiresAt(token.getIssuedAt().plusSeconds(REFRESH_TOKEN_VALIDITY_MINUTES * 60));
        token.setIterationCount(0);
        return refreshTokenRepo.save(token);
    }

    // Verify and update a refresh token if still valid
    public Optional<RefreshToken> verifyRefreshToken(String tokenStr) {
        Optional<RefreshToken> optional = refreshTokenRepo.findByToken(tokenStr);
        if (optional.isEmpty()) return Optional.empty();

        RefreshToken token = optional.get();
        Instant now = Instant.now();

        boolean expired = token.getExpiresAt().isBefore(now);
        boolean idleTooLong = token.getLastUsedAt().plusSeconds(IDLE_EXPIRY_AFTER_ACCESS_MINUTES * 60).isBefore(now);
        boolean overused = token.getIterationCount() >= MAX_ITERATION_COUNT;

        if (expired || idleTooLong || overused) {
            refreshTokenRepo.deleteByToken(tokenStr);
            return Optional.empty();
        }

        // Safe to update usage metadata
        token.setLastUsedAt(now);
        token.setIterationCount(token.getIterationCount() + 1);
        refreshTokenRepo.save(token);

        return Optional.of(token);
    }

    // Delete a refresh token by its string value
    public void deleteByToken(String token) {
        refreshTokenRepo.deleteByToken(token);
    }

    // Save or update a refresh token manually (used in AuthController)
    public RefreshToken save(RefreshToken token) {
        return refreshTokenRepo.save(token);
    }
}
