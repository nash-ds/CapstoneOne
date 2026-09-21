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

import org.springframework.stereotype.Service;

@Service 
public class UserService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository, TokenRepository tokenRepository, JwtService jwtService){
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.jwtService = jwtService;
    }

    public User findByEmail(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("User not found with email: " + email);
        }
        return user;
    }

    public Map<String, String> performLogin(LoginRequest request) {
        User user = findByEmail(request.getEmail());

        if (!user.getPassword().equals(request.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }

        String token = jwtService.generateToken(user);
        registerToken(user.getEmail(), token);
        String refreshToken = jwtService.generateRefreshToken(user);
        registerRefreshToken(user.getEmail(), refreshToken);

        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        body.put("refreshToken", refreshToken);
        return body;
    }

    public Map<String, String> registerUser(LoginRequest loginRequest) {
        if (userRepository.findByEmail(loginRequest.getEmail()) != null) {
            throw new UserAlreadyExistsException("User already registered with email: " + loginRequest.getEmail());
        }

        User user = new User();
        user.setEmail(loginRequest.getEmail());
        user.setPassword(loginRequest.getPassword());
        user.setRoles("USER");
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        registerToken(user.getEmail(), token);
        String refreshToken = jwtService.generateRefreshToken(user);
        registerRefreshToken(user.getEmail(), refreshToken);

        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        body.put("refreshToken", refreshToken);
        return body;
    }

    public String validateAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String email = jwtService.getSubject(token);
        if (!isTokenActive(email, token) || !jwtService.isValid(token)) {
            throw new UnauthorizedException(jwtService.isValidDetailed(token));
        }
        return jwtService.isValidDetailed(token);
    }

    public Map<String, String> refreshAccessToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }

        String refreshToken = authHeader.substring(7);
        String email = jwtService.getSubject(refreshToken);

        if (!jwtService.isValid(refreshToken) || !isRefreshTokenActive(email, refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        User user = findByEmail(email);
        String newAccessToken = jwtService.generateToken(user);
        registerToken(user.getEmail(), newAccessToken);

        Map<String, String> body = new HashMap<>();
        body.put("token", newAccessToken);
        return body;
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String email = jwtService.getSubject(token);
        if (email != null) {
            invalidateToken(email);
            invalidateRefreshToken(email);
        }
    }

    public List<UserResponseDto> getAllUsersForAdmin(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        String email = jwtService.getSubject(token);
        User user = findByEmail(email);
        if (!isAdmin(user)) {
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
