package com.example.demo.repository.employee;

import com.example.demo.model.employee.Employee;
import com.example.demo.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmpIdAndEmail(String empId, String email);
    Optional<Employee> findByEmail(String email);
    Optional<Employee> findByEmpId(String empId);
    List<Employee> findByRole(Role role);

    // Find all employees by status
    List<Employee> findByStatus(String status);

    // Find all by role and status (for filtering active/inactive by role)
    List<Employee> findByRoleAndStatus(Role role, String status);
}