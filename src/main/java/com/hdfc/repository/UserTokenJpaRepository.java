package com.hdfc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hdfc.model.UserToken;

@Repository
public interface UserTokenJpaRepository extends JpaRepository<UserToken, String> {
    
    // Spring Data JPA derives SQL queries directly from method names:
    Optional<UserToken> findByUserEmail(String userEmail);
    
    void deleteByUserEmail(String userEmail);

    Optional<UserToken> findByRefreshToken(String refreshToken);
}