package com.hdfc.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hdfc.model.User;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service 
public class UserService {

    private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();
    private List<User> users = new ArrayList<>();

    @PostConstruct
    public void init() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/users.json")) {
            users = mapper.readValue(is, new TypeReference<List<User>>() {});
            System.out.println(users.get(0));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load users.json", e);
        }
    }

    public User findByEmail(String email) {
        return users.stream()
                    .filter(u -> u.getEmail()
                                    .equalsIgnoreCase(email))
                                    .findFirst()
                                    .orElse(null); // TODO: Add throw exception here when no user found
    }

    public List<User> getAllUsers() { return users; }
    public void registerToken(String token) { activeTokens.add(token); }
    public void invalidateToken(String token) { activeTokens.remove(token); }
    public boolean isTokenActive(String token) { return activeTokens.contains(token); }


}
