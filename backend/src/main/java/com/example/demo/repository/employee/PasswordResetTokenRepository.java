package com.example.demo.repository.employee;

import com.example.demo.model.employee.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Finds a token by its string value.
     *
     * @param token the reset token string
     * @return an Optional containing the matching PasswordResetToken, if any
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Deletes all tokens associated with the given user email.
     *
     * @param email the email of the user
     */
    void deleteByUserEmail(String email);

    /**
     * Deletes the token entry by token string (if exists).
     *
     * @param token the token to be deleted
     */
    void deleteByToken(String token);
}
