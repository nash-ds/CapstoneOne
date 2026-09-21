package com.hdfc.services;

import com.hdfc.exception.InvalidCredentialsException;
import com.hdfc.exception.UnauthorizedException;
import com.hdfc.exception.UserAlreadyExistsException;
import com.hdfc.exception.UserNotFoundException;
import com.hdfc.model.LoginRequest;
import com.hdfc.model.User;
import com.hdfc.model.UserResponseDto;
import com.hdfc.repository.TokenRepository;
import com.hdfc.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service 
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TokenRepository tokenRepository, JwtService jwtService, PasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            log.warn("User not found for email: {}", email);
            throw new UserNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    public Map<String, String> performLogin(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            log.warn("Login attempt with missing email");
            throw new InvalidCredentialsException("Login Failed");
        }

        User user = findByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Incorrect password for email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid password");
        }

        String token = jwtService.generateToken(user);
        registerToken(user.getEmail(), token);
        String refreshToken = jwtService.generateRefreshToken(user);
        registerRefreshToken(user.getEmail(), refreshToken);

        log.info("Login successful for email: {}", user.getEmail());

        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        body.put("refreshToken", refreshToken);
        return body;
    }

    public Map<String, String> registerUser(LoginRequest loginRequest) {
        log.info("Registration attempt for email: {}", loginRequest.getEmail());
        if (userRepository.findByEmail(loginRequest.getEmail()) != null) {
            log.warn("Registration failed - email already exists: {}", loginRequest.getEmail());
            throw new UserAlreadyExistsException("User already registered with email: " + loginRequest.getEmail());
        }

        User user = new User();
        user.setEmail(loginRequest.getEmail());
        user.setPassword(passwordEncoder.encode(loginRequest.getPassword()));
        user.setRoles("USER");
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        registerToken(user.getEmail(), token);
        String refreshToken = jwtService.generateRefreshToken(user);
        registerRefreshToken(user.getEmail(), refreshToken);

        log.info("Registration successful for email: {}", loginRequest.getEmail());

        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        body.put("refreshToken", refreshToken);
        return body;
    }

    public String validateAndExtractEmail(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Token validation failed: missing or malformed Authorization header");
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            log.warn("Token validation failed: empty token");
            throw new UnauthorizedException("Token is empty");
        }

        String status = jwtService.isValidDetailed(token);
        if ("EXPIRED".equals(status)) {
            log.warn("Token validation failed: token has expired");
            throw new UnauthorizedException("Token has expired");
        }
        if (!"VALID".equals(status)) {
            log.warn("Token validation failed: invalid token (status={})", status);
            throw new UnauthorizedException("Invalid token");
        }

        String email = jwtService.getSubject(token);
        if (email == null || !isTokenActive(email, token)) {
            log.warn("Token validation failed: token not found in in-memory storage for email={}", email);
            throw new UnauthorizedException("Token is invalid or not present in in-memory storage");
        }

        log.debug("Token validated successfully for email: {}", email);
        return email;
    }

    public String validateAuth(String authHeader) {
        validateAndExtractEmail(authHeader);
        return "VALID";
    }

    public Map<String, String> refreshAccessToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Refresh failed: missing or malformed Authorization header");
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }

        String refreshToken = authHeader.substring(7);
        String email = jwtService.getSubject(refreshToken);
        if (email == null || !isRefreshTokenActive(email, refreshToken)) {
            log.warn("Refresh failed: refresh token not in in-memory storage for email={}", email);
            throw new UnauthorizedException("Refresh token is invalid or not present in in-memory storage");
        }

        User user = findByEmail(email);
        String newAccessToken = jwtService.generateToken(user);
        registerToken(user.getEmail(), newAccessToken);

        log.info("Access token refreshed successfully for email: {}", email);

        Map<String, String> body = new HashMap<>();
        body.put("token", newAccessToken);
        return body;
    }

    public void logout(String authHeader) {
        String email = validateAndExtractEmail(authHeader);
        invalidateToken(email);
        invalidateRefreshToken(email);
        log.info("Logout successful for email: {}", email);
    }

    public List<UserResponseDto> getAllUsersForAdmin(String authHeader) {
        String email = validateAndExtractEmail(authHeader);
        log.info("Admin request to fetch all users by email: {}", email);
        User user = findByEmail(email);
        if (!isAdmin(user)) {
            log.warn("Unauthorized admin access attempt by email: {}", email);
            throw new UnauthorizedException("Not authorized to view all users");
        }
        return getAllUsers();
    }

    public boolean isAdmin(User user){
        if (user != null && user.getRoles() != null && user.getRoles().contains("ADMIN")){
            return true;
        }
        return false;
    }

    public List<UserResponseDto> getAllUsers() { 
        List<User> users = userRepository.findAll();
        log.debug("Fetched {} users from database", users.size());
        return users.stream()
                .map(user -> new UserResponseDto(
                        user.getUserId(),
                        user.getEmail(),
                        user.getRoles()
                ))
                .collect(Collectors.toList());
    }

    public void save(User user){ userRepository.save(user); }
    public void registerToken(String email, String token) { tokenRepository.registerToken(email , token); }
    public void invalidateToken(String email) { tokenRepository.invalidateToken(email); }
    public boolean isTokenActive(String email ,String token) { return tokenRepository.isTokenActive(email ,token); }
    public void registerRefreshToken(String email ,String token) { tokenRepository.registerRefreshToken(email ,token); }
    public void invalidateRefreshToken(String email) { tokenRepository.invalidateRefreshToken(email); }
    public boolean isRefreshTokenActive(String email ,String token) { return tokenRepository.isRefreshTokenActive(email ,token); }
}
