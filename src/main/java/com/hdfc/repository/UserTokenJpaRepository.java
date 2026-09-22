package com.hdfc.repository;

import java.util.Optional;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hdfc.model.UserToken;

@Repository
public interface UserTokenJpaRepository extends JpaRepository<UserToken, String> {
    
    Optional<UserToken> findByUserEmail(String userEmail);
    
    void deleteByUserEmail(String userEmail);

    Optional<UserToken> findByRefreshToken(String refreshToken);

    @Transactional
    @Modifying
    @Query(value = "TRUNCATE TABLE user_tokens RESTART IDENTITY CASCADE", nativeQuery = true)
    void truncateTable();

    // Runs automatically when the Spring application context starts up
    @EventListener(ContextRefreshedEvent.class)
    default void onStartup() {
        truncateTable();
    }
}