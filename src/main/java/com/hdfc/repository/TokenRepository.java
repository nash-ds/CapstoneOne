package com.hdfc.repository;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class TokenRepository {

    private final Map<String , String> activeTokens = new ConcurrentHashMap<>();
    private final Map<String , String> refreshTokens = new ConcurrentHashMap<>();

    public void registerToken(String email , String token) {
        activeTokens.put(email, token);
    }

    public void invalidateToken(String email) {
        activeTokens.remove(email);
    }

    public boolean isTokenActive(String email, String token) {
        return token.equals(activeTokens.get(email));
    }
    public void registerRefreshToken(String email, String refreshToken) { refreshTokens.put(email ,refreshToken); }

    public void invalidateRefreshToken(String email) { refreshTokens.remove(email); }

    public boolean isRefreshTokenActive(String email, String refreshToken) {
        return refreshToken.equals(refreshTokens.get(email));
    }
}
