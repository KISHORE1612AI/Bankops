package com.example.demo.controller.employee;

import com.example.demo.dto.admin.SuperAdminUserDto;
import com.example.demo.service.employee.SuperAdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/superadmin/users")
public class SuperAdminUserController {

    private final SuperAdminUserService userService;

    @Autowired
    public SuperAdminUserController(SuperAdminUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<SuperAdminUserDto> getAllUsers() {
        return userService.getAllUsers();
    }

    // You can add POST, PUT, DELETE endpoints here if you want to support user management (add/edit/delete)
}