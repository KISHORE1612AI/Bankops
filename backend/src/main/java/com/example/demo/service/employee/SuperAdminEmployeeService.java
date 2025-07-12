package com.example.demo.service.employee;

import com.example.demo.dto.admin.SuperAdminEmployeeDto;
import java.util.List;

public interface SuperAdminEmployeeService {
    List<SuperAdminEmployeeDto> getAllEmployees();
    // Optionally add: SuperAdminEmployeeDto getEmployeeById(Long id), void deleteEmployee(Long id), etc.
}