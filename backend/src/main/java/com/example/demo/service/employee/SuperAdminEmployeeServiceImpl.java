package com.example.demo.service.employee;

import com.example.demo.dto.admin.SuperAdminEmployeeDto;
import com.example.demo.model.employee.Employee;
import com.example.demo.repository.employee.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuperAdminEmployeeServiceImpl implements SuperAdminEmployeeService {
    private final EmployeeRepository employeeRepository;

    @Autowired
    public SuperAdminEmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public List<SuperAdminEmployeeDto> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        // Map Employee -> SuperAdminEmployeeDto
        return employees.stream()
                .map(emp -> new SuperAdminEmployeeDto(
                        emp.getId(),
                        emp.getName(),
                        emp.getRole() != null ? emp.getRole().name() : null,
                        emp.getStatus() != null && emp.getStatus().equalsIgnoreCase("ACTIVE")
                ))
                .collect(Collectors.toList());
    }
}