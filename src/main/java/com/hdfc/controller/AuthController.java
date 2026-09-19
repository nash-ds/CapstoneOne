package com.hdfc.controller;

import com.hdfc.exception.AuthException;
import com.hdfc.model.LoginRequest;
import com.hdfc.model.User;
import com.hdfc.services.JwtService;
import com.hdfc.services.UserService;
import com.hdfc.utility.ApiResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class AuthController {

    private static final Logger log =
            LoggerFactory.getLogger(AuthController.class);

    private final JwtService jwtService;
    private final UserService userService;


    public AuthController(
            JwtService jwtService,
            UserService userService) {

        this.jwtService = jwtService;
        this.userService = userService;
    }


    // =========================================================
    // LOGIN
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, String>>> login(
            @RequestBody LoginRequest request) {

        log.info("Login request received");

        if (request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {

            throw AuthException.emailRequired();
        }

        if (request.getPassword() == null ||
                request.getPassword().isEmpty()) {

            throw AuthException.passwordRequired();
        }

        User user =
                userService.findByEmail(request.getEmail());

        if (user == null) {

            throw AuthException.emailNotRegistered();
        }

        if (!user.getPassword().equals(request.getPassword())) {

            throw AuthException.incorrectPassword();
        }

        String token =
                jwtService.generateToken(user);

        userService.registerToken(token);

        String refreshToken =
                jwtService.generateRefreshToken(user);

        userService.registerRefreshToken(refreshToken);

        Map<String, String> body =
                new HashMap<>();

        body.put("token", token);
        body.put("refreshToken", refreshToken);

        log.info("Login successful");

        ApiResponse<Map<String, String>> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Login Successful",
                        body
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // REGISTER
    // =========================================================

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, String>>> register(
            @RequestBody User user) {

        log.info("Registration request received");

        if (user.getEmail() == null ||
                user.getEmail().trim().isEmpty()) {

            throw AuthException.emailRequired();
        }

        if (user.getPassword() == null ||
                user.getPassword().isEmpty()) {

            throw AuthException.passwordRequired();
        }

        User existingUser =
                userService.findByEmail(user.getEmail());

        if (existingUser != null) {

            throw AuthException.emailAlreadyRegistered();
        }

        userService.save(user);

        String token =
                jwtService.generateToken(user);

        userService.registerToken(token);

        String refreshToken =
                jwtService.generateRefreshToken(user);

        userService.registerRefreshToken(refreshToken);

        Map<String, String> body =
                new HashMap<>();

        body.put("token", token);
        body.put("refreshToken", refreshToken);

        log.info("Registration successful");

        ApiResponse<Map<String, String>> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Registration Successful",
                        body
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // AUTH
    // =========================================================

    @GetMapping("/auth")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<String>> validateAuth(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authHeader) {

        log.info("Authentication validation request received");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            throw AuthException.authorizationTokenRequired();
        }

        String token =
                authHeader.substring(7);

        if (token.trim().isEmpty()) {

            throw AuthException.authorizationTokenEmpty();
        }

        if (!userService.isTokenActive(token)) {

            throw AuthException.tokenInactive();
        }

        if ("EXPIRED".equals(
                jwtService.isValidDetailed(token))) {

            throw AuthException.tokenExpired();
        }

        if (!jwtService.isValid(token)) {

            throw AuthException.invalidAuthenticationToken();
        }

        log.info("Authentication successful");

        ApiResponse<String> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "VALID",
                        "Authentication successful"
                );

        return ResponseEntity.ok(response);
    }


    // =========================================================
    // REFRESH
    // =========================================================

    @GetMapping("/refresh")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authHeader) {

        log.info("Refresh token request received");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            throw AuthException.refreshTokenRequired();
        }

        String refreshToken =
                authHeader.substring(7);

        if (refreshToken.trim().isEmpty()) {

            throw AuthException.refreshTokenEmpty();
        }

        if (!userService.isRefreshTokenActive(refreshToken)) {

            throw AuthException.refreshTokenInactive();
        }

        if ("EXPIRED".equals(
                jwtService.isValidDetailed(refreshToken))) {

            throw AuthException.refreshTokenExpired();
        }

        if (!jwtService.isValid(refreshToken)) {

            throw AuthException.invalidRefreshToken();
        }

        String email =
                jwtService.getSubject(refreshToken);

        User user =
                userService.findByEmail(email);

        if (user == null) {

            throw AuthException.refreshUserNotFound();
        }

        String newAccessToken =
                jwtService.generateToken(user);

        userService.registerToken(newAccessToken);

        Map<String, String> body =
                new HashMap<>();

        body.put("token", newAccessToken);
        body.put("refreshToken", refreshToken);

        log.info("Access token refreshed successfully");

        ApiResponse<Map<String, String>> response =
                ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "Token refreshed successfully",
                        body
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // LOGOUT
    // =========================================================

    @PostMapping("/logoutUser")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authHeader) {

        log.info("Logout request received");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            throw AuthException.authorizationTokenRequired();
        }

        String token =
                authHeader.substring(7);

        if (token.trim().isEmpty()) {

            throw AuthException.authorizationTokenEmpty();
        }

        if (!userService.isTokenActive(token)) {

            throw AuthException.logoutTokenInactive();
        }

        userService.invalidateToken(token);

        log.info("Logout successful");

        ApiResponse<String> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Logged out successfully",
                        null
                );

        return ResponseEntity.ok(response);
    }
}