package com.hdfc.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfc.model.User;
import com.hdfc.repository.UserRepository;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service 
public class UserService {

    // private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();
    // private List<User> users = new ArrayList<>();

    private UserRepository userRepository;

    public UserService(UserRepository userRepository){
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/users.json")) {
            List<User> users = mapper.readValue(is, new TypeReference<List<User>>() {});
            userRepository.saveAll(users);
            System.out.println("##### Users loaded: "+users.get(0));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load users.json", e);
        }
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getAllUsers() { return userRepository.findAll(); }
    public void save(User user){ userRepository.save(user); }
    public void registerToken(String token) { userRepository.registerToken(token); }
    public void invalidateToken(String token) { userRepository.invalidateToken(token); }
    public boolean isTokenActive(String token) { return userRepository.isTokenActive(token); }
    public void registerRefreshToken(String token) { userRepository.registerRefreshToken(token); }
    public void invalidateRefreshToken(String token) { userRepository.invalidateRefreshToken(token); }
    public boolean isRefreshTokenActive(String token) { return userRepository.isRefreshTokenActive(token); }



}
