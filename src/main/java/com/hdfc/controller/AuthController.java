package com.hdfc.controller;

import com.hdfc.model.LoginRequest;
import com.hdfc.model.User;
import com.hdfc.services.JwtService;
import com.hdfc.services.UserService;
import com.hdfc.utility.ApiResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.hdfc.services.ResilientLoginService;


@RestController
public class AuthController {

    private final JwtService jwtService;
    private final UserService userService;
    private final ResilientLoginService resilientLoginService;


    AuthController(JwtService jwtService, UserService userService,ResilientLoginService resilientLoginService){
        this.jwtService = jwtService;
        this.userService = userService;
        this.resilientLoginService = resilientLoginService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(@RequestBody LoginRequest request) {
        System.out.println("entered func");
        User user = userService.findByEmail(request.getEmail());

        if (user != null && user.getPassword().equals(request.getPassword())) {
            String token = jwtService.generateToken(user);
            userService.registerToken(token);
            String refreshToken = jwtService.generateRefreshToken(user);
            userService.registerRefreshToken(refreshToken);
            Map<String, String> body = new HashMap<>();
            body.put("token", token);
            body.put("refreshToken",refreshToken);
            ApiResponse<Map<String, String>> response = ApiResponse.success(HttpStatus.OK.value(),"Login Successful",body);
            return ResponseEntity.ok(response);
        }
        ApiResponse<Map<String, String>> response = ApiResponse.error(HttpStatus.UNAUTHORIZED.value(),"Login Failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        //TODO: Error handling
        userService.save(user);
        String token = jwtService.generateToken(user);
        userService.registerToken(token);
        String refreshToken = jwtService.generateRefreshToken(user);
        userService.registerRefreshToken(refreshToken);
        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        body.put("refreshToken",refreshToken);
        ApiResponse<Map<String, String>> response = ApiResponse.success(HttpStatus.OK.value(),"Registration Successful",body);
        return ResponseEntity.ok(response);
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
//            String email = jwtService.getSubject(token);
            ApiResponse response = ApiResponse.success(HttpStatus.OK.value(), jwtService.isValidDetailed(token), true);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        ApiResponse response = ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), jwtService.isValidDetailed(token));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @GetMapping("/refresh")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Map<String,String>>> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ApiResponse response = ApiResponse.error(HttpStatus.BAD_REQUEST.value(),"Missing token");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        String refreshToken = authHeader.substring(7);

        if (refreshToken != null && jwtService.isValid(refreshToken) && userService.isRefreshTokenActive(refreshToken)) {

            String email = jwtService.getSubject(refreshToken);
            User user = userService.findByEmail(email);

            if (user != null) {
                // 2. Generate a new Access Token (and optionally rotate the refresh token)
                String newAccessToken = jwtService.generateToken(user);
                Map<String, String> body = new HashMap<>();
                body.put("token", newAccessToken);
                userService.registerToken(newAccessToken);
                body.put("refreshToken",refreshToken);
                ApiResponse authResponse = ApiResponse.success(HttpStatus.CREATED.value(), "Token refreshed successfully",body);
                return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
            }
        }

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired refresh token"));
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

    @GetMapping("/user")
    @SecurityRequirement(name = "bearerAuth")
    public User getUser(@RequestHeader(value = "Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String email = jwtService.getSubject(token);
            User user = userService.findByEmail(email);
            return user;    
        }
        return null;
    }
    
}
