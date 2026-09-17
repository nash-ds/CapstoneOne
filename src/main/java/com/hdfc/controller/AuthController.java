package com.hdfc.controller;

import com.hdfc.model.LoginRequest;
import com.hdfc.model.User;
import com.hdfc.services.JwtService;
import com.hdfc.services.UserService;
import com.hdfc.utility.ApiResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class AuthController {

    private final JwtService jwtService;
    private final UserService userService;
    

    AuthController(JwtService jwtService, UserService userService){
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        System.out.println("entered func");
        User user = userService.findByEmail(request.getEmail());

        if (user != null && user.getPassword().equals(request.getPassword())) {
            String token = jwtService.generateToken(user);
            userService.registerToken(token);

            Map<String, String> body = new HashMap<>();
            body.put("token", token);
            ApiResponse<Map<String, String>> response = ApiResponse.success(HttpStatus.OK.value(),"Login Successful",body);
            return ResponseEntity.ok(response);
        }
        ApiResponse<Map<String, String>> response = ApiResponse.error(HttpStatus.UNAUTHORIZED.value(),"Login Failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        userService.save(user);
        return ResponseEntity.ok("User registered successfully");
    }
    
    @GetMapping("/auth")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<String>> validateAuth(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(),"Missing token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        String token = authHeader.substring(7);
        if (userService.isTokenActive(token) && jwtService.isValid(token)) {
            String email = jwtService.getSubject(token);
            ApiResponse response = ApiResponse.success(HttpStatus.OK.value(), "Token is Valid",email);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        ApiResponse response = ApiResponse.error(HttpStatus.UNAUTHORIZED.value(),"User Not Authorised");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/logoutUser")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userService.invalidateToken(token);
        }
        return ResponseEntity.ok("Logged out successfully");
    }
}
