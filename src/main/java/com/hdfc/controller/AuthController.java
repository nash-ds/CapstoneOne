package com.hdfc.controller;

import com.hdfc.model.LoginRequest;
import com.hdfc.model.UserResponseDto;
import com.hdfc.services.JwtService;
import com.hdfc.services.UserRateLimiter;
import com.hdfc.services.UserService;
import com.hdfc.utility.ApiResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final JwtService jwtService;
    private final UserService userService;
    private final UserRateLimiter userRateLimiter;

    public AuthController(JwtService jwtService, UserService userService, UserRateLimiter userRateLimiter) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRateLimiter = userRateLimiter;
    }

    @CircuitBreaker(name = "dbBreaker", fallbackMethod = "dbFallback")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return userRateLimiter.hitPerUser(request.getEmail(), () -> {
            Map<String, String> body = userService.performLogin(request);
            ApiResponse<Map<String, String>> response = ApiResponse.success(HttpStatus.OK.value(), "Login Successful", body);
            return ResponseEntity.ok(response);
        });
    }

    // Rate Limiter Fallback (429)
    public ResponseEntity<?> findByEmailFallbackRL(LoginRequest request, RequestNotPermitted throwable) {
        ApiResponse<String> response = ApiResponse.error(
            HttpStatus.TOO_MANY_REQUESTS.value(), 
            "Too many login attempts. Please try again after 1 minute."
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    // Circuit Breaker Fallback (503)
    public ResponseEntity<?> dbFallback(LoginRequest request, Throwable t) {
        System.out.println(">>> CIRCUIT BREAKER TRIGGERED! Error: " + t.getMessage());
        ApiResponse<String> response = ApiResponse.error(
            HttpStatus.SERVICE_UNAVAILABLE.value(), 
            "Database service is currently unavailable. Please try again later."
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest loginRequest) {
        Map<String, String> body = userService.registerUser(loginRequest);
        ApiResponse<Map<String, String>> response = ApiResponse.success(HttpStatus.CREATED.value(), "Registration Successful", body);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/auth")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> validateAuth(@RequestHeader("Authorization") String authHeader) {
        String detail = userService.validateAuth(authHeader);
        ApiResponse<Boolean> response = ApiResponse.success(HttpStatus.OK.value(), detail, true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/refresh")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        Map<String, String> body = userService.refreshAccessToken(authHeader);
        ApiResponse<Map<String, String>> authResponse = ApiResponse.success(HttpStatus.CREATED.value(), "Token refreshed successfully", body);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization") String accessHeader) {
        userService.logout(accessHeader);
        ApiResponse<String> response = ApiResponse.success(HttpStatus.OK.value(), "Logged out successfully", null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<?> getAllUsers(@RequestHeader(value = "Authorization") String authHeader) {
        List<UserResponseDto> users = userService.getAllUsersForAdmin(authHeader);
        ApiResponse<List<UserResponseDto>> response = ApiResponse.success(HttpStatus.OK.value(), "Users fetched successfully", users);
        return ResponseEntity.ok(response);
    }
}
