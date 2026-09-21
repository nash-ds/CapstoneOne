package com.hdfc.repository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.hdfc.model.UserToken;

@Repository
public class TokenRepository {

    private final UserTokenJpaRepository jpaRepository;

    public TokenRepository(UserTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    public void registerToken(String email, String token) {
        UserToken userToken = jpaRepository.findByUserEmail(email)
                .orElseGet(() -> new UserToken(email, null, null));
        
        userToken.setAccessToken(token);
        jpaRepository.save(userToken);
    }

    // Clear Access Token
    public void invalidateToken(String email) {
        jpaRepository.findByUserEmail(email).ifPresent(userToken -> {
            userToken.setAccessToken(null);
            jpaRepository.save(userToken);
        });
    }

    // Check if Access Token matches current active token
    public boolean isTokenActive(String email, String token) {
        return jpaRepository.findByUserEmail(email)
                .map(UserToken::getAccessToken)
                .map(storedToken -> storedToken.equals(token))
                .orElse(false);
    }

    // Upsert Refresh Token
    public void registerRefreshToken(String email, String refreshToken) {
        UserToken userToken = jpaRepository.findByUserEmail(email)
                .orElseGet(() -> new UserToken(email, null, null));

        userToken.setRefreshToken(refreshToken);
        jpaRepository.save(userToken);
    }

    // Clear Refresh Token
    public void invalidateRefreshToken(String email) {
        jpaRepository.findByUserEmail(email).ifPresent(userToken -> {
            userToken.setRefreshToken(null);
            jpaRepository.save(userToken);
        });
    }

    // Check if Refresh Token matches
    public boolean isRefreshTokenActive(String email, String refreshToken) {
        return jpaRepository.findByUserEmail(email)
                .map(UserToken::getRefreshToken)
                .map(storedToken -> storedToken.equals(refreshToken))
                .orElse(false);
    }

    public Optional<UserToken> findByRefreshToken(String refreshToken) {
        return jpaRepository.findByRefreshToken(refreshToken);
    }

    public boolean isUserActive(String email) {
        return jpaRepository.findByUserEmail(email)
                .map(token -> token.getAccessToken() != null || token.getRefreshToken() != null)
                .orElse(false);
    }
}
