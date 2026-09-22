package com.hdfc.services;

import com.hdfc.exception.InvalidCredentialsException;
import com.hdfc.exception.UnauthorizedException;
import com.hdfc.exception.UserAlreadyExistsException;
import com.hdfc.exception.UserNotFoundException;
import com.hdfc.model.LoginRequest;
import com.hdfc.model.User;
import com.hdfc.model.UserResponseDto;
import com.hdfc.model.UserToken;
import com.hdfc.repository.TokenRepository;
import com.hdfc.repository.UserRepository;

import java.util.*;

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
    private final UserRateLimiter userRateLimiter;

    public UserService(UserRepository userRepository, TokenRepository tokenRepository, JwtService jwtService, PasswordEncoder passwordEncoder, UserRateLimiter userRateLimiter){
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userRateLimiter = userRateLimiter;
    }

    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            log.warn("User not found for email: {}", email);
            throw new UserNotFoundException("Invalid Login Credentials");
        }
        return user;
    }

    public Map<String, String> performLogin(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            log.warn("Login attempt with missing email");
            throw new InvalidCredentialsException("Invalid Login Credentials");
        }

        User user = findByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Incorrect password for email: {}", request.getEmail());
            throw new InvalidCredentialsException("Invalid Login Credentials");
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
            throw new UserAlreadyExistsException("Invalid Login Credentials ");
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
            throw new UnauthorizedException("Token is not present in in-memory");
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

    String refreshToken = authHeader.substring(7).trim();

    UserToken userToken = tokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> {
                log.warn("Refresh failed: refresh token not found in database");
                return new UnauthorizedException("Invalid refresh token");
            });

    String refreshStatus = jwtService.isValidDetailed(refreshToken);
    if (!"VALID".equals(refreshStatus)) {
        log.warn("Refresh failed: refresh token status is {}", refreshStatus);
        throw new UnauthorizedException("Refresh token is expired or invalid");
    }

    String currentAccessToken = userToken.getAccessToken();
    if (currentAccessToken != null) {
        String accessTokenStatus = jwtService.isValidDetailed(currentAccessToken);
        
        if ("VALID".equals(accessTokenStatus)) {
            log.warn("Refresh rejected: Current access token for email {} is still active", userToken.getUserEmail());
            throw new UnauthorizedException("Access token is still active. Refresh not permitted yet.");
        }
    }

    User user = findByEmail(userToken.getUserEmail());
    String newAccessToken = jwtService.generateToken(user);
    registerToken(user.getEmail(), newAccessToken);

    log.info("Access token refreshed successfully for email: {}", user.getEmail());

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
        return users.stream().map(u -> {
            boolean isLocked = userRateLimiter.isUserLocked(u.getEmail());
            boolean active = tokenRepository.isUserActive(u.getEmail());
            String status;
            if (isLocked) {
                status = "LOCKED";
            } else if (active) {
                status = "ACTIVE";
            } else {
                status = "INACTIVE";
            }

            return new UserResponseDto(
                u.getUserId(),
                u.getEmail(),
                u.getRoles(),
                status
            );
        }).toList();
    }

    public void save(User user){ userRepository.save(user); }
    public void registerToken(String email, String token) { tokenRepository.registerToken(email , token); }
    public void invalidateToken(String email) { tokenRepository.invalidateToken(email); }
    public boolean isTokenActive(String email ,String token) { return tokenRepository.isTokenActive(email ,token); }
    public void registerRefreshToken(String email ,String token) { tokenRepository.registerRefreshToken(email ,token); }
    public void invalidateRefreshToken(String email) { tokenRepository.invalidateRefreshToken(email); }
    public boolean isRefreshTokenActive(String email ,String token) { return tokenRepository.isRefreshTokenActive(email ,token); }
}
