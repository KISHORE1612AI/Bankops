package com.example.demo.repository.employee;

import com.example.demo.model.employee.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUserEmail(String userEmail);
    void deleteByToken(String token);

    // 🆕 Update lastUsedAt and increment iteration count
    @Modifying
    @Query("UPDATE RefreshToken r SET r.lastUsedAt = :lastUsedAt, r.iterationCount = r.iterationCount + 1 WHERE r.token = :token")
    void updateUsageMetadata(String token, Instant lastUsedAt);
}
