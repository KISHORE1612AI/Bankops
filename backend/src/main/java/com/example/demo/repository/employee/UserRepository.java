package com.example.demo.repository.employee;

import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Add custom queries if needed, for example:
    // List<User> findByActiveTrue();
}