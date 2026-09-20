package com.hdfc.controller;

import com.hdfc.exception.AuthException;
import com.hdfc.model.LoginRequest;
import com.hdfc.model.User;
import com.hdfc.services.JwtService;
import com.hdfc.services.ResilientLoginService;
import com.hdfc.services.UserService;
import com.hdfc.utility.ApiResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.HashMap;
import java.util.List;
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
    private final ResilientLoginService resilientLoginService;

    public AuthController(
            JwtService jwtService,
            UserService userService,
            ResilientLoginService resilientLoginService) {

        this.jwtService = jwtService;
        this.userService = userService;
        this.resilientLoginService = resilientLoginService;
    }

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

        resilientLoginService.authenticate(
                request.getEmail(),
                request.getPassword(),
                false
        );

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

        userService.registerToken(
                user.getEmail(),
                token
        );

        String refreshToken =
                jwtService.generateRefreshToken(user);

        userService.registerRefreshToken(
                user.getEmail(),
                refreshToken
        );

        Map<String, String> body =
                new HashMap<>();

        body.put("token", token);
        body.put("refreshToken", refreshToken);

        log.info(
                "Login successful for email: {}",
                user.getEmail()
        );

        ApiResponse<Map<String, String>> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Login Successful",
                        body
                );

        return ResponseEntity.ok(response);
    }

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

        userService.registerToken(
                user.getEmail(),
                token
        );

        String refreshToken =
                jwtService.generateRefreshToken(user);

        userService.registerRefreshToken(
                user.getEmail(),
                refreshToken
        );

        Map<String, String> body =
                new HashMap<>();

        body.put("token", token);
        body.put("refreshToken", refreshToken);

        log.info(
                "Registration successful for email: {}",
                user.getEmail()
        );

        ApiResponse<Map<String, String>> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Registration Successful",
                        body
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/auth")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<String>> validateAuth(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authHeader) {

        log.info("Authentication validation request received");

        String token = extractBearerToken(authHeader);

        validateAccessToken(token);

        log.info("Authentication successful");

        ApiResponse<String> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "VALID",
                        "Authentication successful"
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/refresh")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authHeader) {

        log.info("Refresh token request received");

        String refreshToken =
                extractRefreshToken(authHeader);

        String validationStatus =
                jwtService.isValidDetailed(refreshToken);

        if ("EXPIRED".equals(validationStatus)) {
            throw AuthException.refreshTokenExpired();
        }

        if (!"VALID".equals(validationStatus)) {
            throw AuthException.invalidRefreshToken();
        }

        String email =
                jwtService.getSubject(refreshToken);

        if (!userService.isRefreshTokenActive(
                email,
                refreshToken)) {

            throw AuthException.refreshTokenInactive();
        }

        User user =
                userService.findByEmail(email);

        if (user == null) {
            throw AuthException.refreshUserNotFound();
        }

        String newAccessToken =
                jwtService.generateToken(user);

        userService.registerToken(
                email,
                newAccessToken
        );

        Map<String, String> body =
                new HashMap<>();

        body.put("token", newAccessToken);
        body.put("refreshToken", refreshToken);

        log.info(
                "Access token refreshed successfully for email: {}",
                email
        );

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

    @PostMapping("/logoutUser")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authHeader) {

        log.info("Logout request received");

        String token =
                extractBearerToken(authHeader);

        String email =
                validateAccessToken(token);

        if (!userService.isTokenActive(
                email,
                token)) {

            throw AuthException.logoutTokenInactive();
        }

        userService.logout(token);

        log.info(
                "Logout successful for email: {}",
                email
        );

        ApiResponse<String> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Logged out successfully",
                        null
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/user")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<User>> getUser(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authHeader) {

        log.info("Get user request received");

        String token =
                extractBearerToken(authHeader);

        String email =
                validateAccessToken(token);

        User user =
                userService.findByEmail(email);

        if (user == null) {
            throw AuthException.userNotFound();
        }

        log.info(
                "User details fetched successfully for email: {}",
                email
        );

        ApiResponse<User> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "User fetched successfully",
                        user
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-circuit-breaker")
    public ResponseEntity<ApiResponse<String>> testCircuitBreaker() {

        log.info("CircuitBreaker test request received");

        resilientLoginService.circuitProtectedLogin(
                "prasad@gmail.com",
                "prasad123",
                true
        );

        ApiResponse<String> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "External service successful",
                        "CircuitBreaker is CLOSED"
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            )
            String authHeader) {

        log.info("Get all users request received");

        String token =
                extractBearerToken(authHeader);

        String email =
                validateAccessToken(token);

        User user =
                userService.findByEmail(email);

        if (user == null) {
            throw AuthException.userNotFound();
        }

        if (!userService.isAdmin(user)) {
            throw AuthException.notAuthorizedToViewUsers();
        }

        List<User> users =
                userService.getAllUsers();

        log.info("All users fetched successfully");

        ApiResponse<List<User>> response =
                ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Users fetched successfully",
                        users
                );

        return ResponseEntity.ok(response);
    }

    private String extractBearerToken(String authHeader) {

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            throw AuthException.authorizationTokenRequired();
        }

        String token =
                authHeader.substring(7);

        if (token.trim().isEmpty()) {
            throw AuthException.authorizationTokenEmpty();
        }

        return token;
    }

    private String extractRefreshToken(String authHeader) {

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            throw AuthException.refreshTokenRequired();
        }

        String refreshToken =
                authHeader.substring(7);

        if (refreshToken.trim().isEmpty()) {
            throw AuthException.refreshTokenEmpty();
        }

        return refreshToken;
    }

    private String validateAccessToken(String token) {

        String validationStatus =
                jwtService.isValidDetailed(token);

        if ("EXPIRED".equals(validationStatus)) {
            throw AuthException.tokenExpired();
        }

        if (!"VALID".equals(validationStatus)) {
            throw AuthException.invalidAuthenticationToken();
        }

        String email =
                jwtService.getSubject(token);

        if (!userService.isTokenActive(
                email,
                token)) {

            throw AuthException.tokenInactive();
        }

        return email;
    }
}