package com.example.demo.service.employee;

import com.example.demo.dto.admin.SuperAdminUserDto;
import com.example.demo.model.User;
import com.example.demo.repository.employee.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuperAdminUserServiceImpl implements SuperAdminUserService {

    private final UserRepository userRepository;

    @Autowired
    public SuperAdminUserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<SuperAdminUserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        // Map User -> SuperAdminUserDto
        return users.stream()
                .map(user -> new SuperAdminUserDto(
                        user.getId(),
                        user.getFullName(), // Adjust if your field is different
                        user.getEmail(),
                        user.getActive(),   // or user.isActive()
                        user.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}