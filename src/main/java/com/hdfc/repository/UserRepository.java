package com.hdfc.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.hdfc.model.User;

@Repository 
public class UserRepository {

    private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();
    private final Set<String> refreshTokens = ConcurrentHashMap.newKeySet();
    private List<User> users = new ArrayList<>();

    public User findByEmail(String email) {
        return users.stream()
                .filter(u -> u.getEmail().equals(email))
                .findFirst()
                .orElse(null); // TODO: Add throw exception here when no user found
    }

    public void registerToken(String token) {
        activeTokens.add(token);
    }

    public void invalidateToken(String token) {
        activeTokens.remove(token);
    }

    public boolean isTokenActive(String token) {
        return activeTokens.contains(token);
    }

    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    public void save(User user){
        users.add(user);
    }

    public void saveAll(List<User> users){
        this.users.addAll(users);
    }

    public void update(int id , User user){
        users.set(id, user);
    }

    public void deleteById(int id){
        users.remove(id);
    }

    public void registerRefreshToken(String refreshToken) { refreshTokens.add(refreshToken); }

    public void invalidateRefreshToken(String refreshToken) { refreshTokens.remove(refreshToken); }

    public boolean isRefreshTokenActive(String refreshToken) { return refreshTokens.contains(refreshToken); }

    public void patch(int id, User user){
        User u = users.get(id);

        if(user.getEmail() != null && !user.getEmail().isEmpty()){
            u.setEmail(user.getEmail());
        } 
        if(user.getPassword() != null && !user.getPassword().isEmpty()){
            u.setPassword(user.getPassword());
        }
        if(user.getRoles() != null && !user.getRoles().isEmpty()){
            u.setRoles(user.getRoles());
        }
    }
}
