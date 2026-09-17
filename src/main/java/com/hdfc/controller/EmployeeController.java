package com.hdfc.controller;

import com.hdfc.model.User;
import com.hdfc.services.UserService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class EmployeeController {

    private final UserService userService;

    public EmployeeController(UserService userService) {
        this.userService = userService;
    }

    // @GetMapping("/profile")
    // public ResponseEntity<?> getProfile() {
    //     Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    //     if (authentication == null || !authentication.isAuthenticated()) {
    //         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
    //     }

    //     String userEmail = (String) authentication.getPrincipal();

    //     return userService.findByEmail(userEmail);
    // }

    @GetMapping("/temp")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }
    
}