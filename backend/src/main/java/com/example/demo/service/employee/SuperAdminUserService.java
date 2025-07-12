package com.example.demo.service.employee;

import com.example.demo.dto.admin.SuperAdminUserDto;
import java.util.List;

public interface SuperAdminUserService {
    List<SuperAdminUserDto> getAllUsers();
    // Optionally add: SuperAdminUserDto getUserById(Long id), void deleteUser(Long id), etc.
}