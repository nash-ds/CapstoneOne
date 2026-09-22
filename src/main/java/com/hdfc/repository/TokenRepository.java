package com.hdfc.repository;

import java.util.Optional;

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

    public void invalidateToken(String email) {
        jpaRepository.findByUserEmail(email).ifPresent(userToken -> {
            userToken.setAccessToken(null);
            jpaRepository.save(userToken);
        });
    }

    public boolean isTokenActive(String email, String token) {
        return jpaRepository.findByUserEmail(email)
                .map(UserToken::getAccessToken)
                .map(storedToken -> storedToken.equals(token))
                .orElse(false);
    }

    public void registerRefreshToken(String email, String refreshToken) {
        UserToken userToken = jpaRepository.findByUserEmail(email)
                .orElseGet(() -> new UserToken(email, null, null));

        userToken.setRefreshToken(refreshToken);
        jpaRepository.save(userToken);
    }

    public void invalidateRefreshToken(String email) {
        jpaRepository.findByUserEmail(email).ifPresent(userToken -> {
            userToken.setRefreshToken(null);
            jpaRepository.save(userToken);
        });
    }

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
