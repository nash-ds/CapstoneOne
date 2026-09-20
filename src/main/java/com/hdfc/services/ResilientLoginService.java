package com.hdfc.services;

import com.hdfc.exception.CircuitBreakerException;
import com.hdfc.exception.RateLimitExceededException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

@Service
public class ResilientLoginService {

    private static final Logger log =
            LoggerFactory.getLogger(ResilientLoginService.class);

    private final MockExternalLoginService externalLoginService;

    public ResilientLoginService(
            MockExternalLoginService externalLoginService) {

        this.externalLoginService = externalLoginService;
    }

    @RateLimiter(
            name = "loginRateLimiter",
            fallbackMethod = "rateLimiterFallback"
    )
    public boolean authenticate(
            String email,
            String password,
            boolean simulateFailure) {

        log.info(
                "RateLimiter checking login request for: {}",
                email
        );

        return circuitProtectedLogin(
                email,
                password,
                simulateFailure
        );
    }

    @CircuitBreaker(
            name = "externalLoginService",
            fallbackMethod = "circuitBreakerFallback"
    )
    public boolean circuitProtectedLogin(
            String email,
            String password,
            boolean simulateFailure) {

        log.info(
                "Calling external login service for: {}",
                email
        );

        return externalLoginService.authenticate(
                email,
                password,
                simulateFailure
        );
    }

    public boolean rateLimiterFallback(
            String email,
            String password,
            boolean simulateFailure,
            Throwable exception) {

        log.warn(
                "Rate limiter fallback triggered for email: {}. Reason: {}",
                email,
                exception.getMessage()
        );

        throw new RateLimitExceededException(
                "Too many login requests. Please try again later."
        );
    }

    public boolean circuitBreakerFallback(
            String email,
            String password,
            boolean simulateFailure,
            Throwable exception) {

        log.error(
                "Circuit breaker fallback triggered for email: {}. Reason: {}",
                email,
                exception.getMessage()
        );

        throw new CircuitBreakerException(
                "Login service is temporarily unavailable. Please try again later."
        );
    }
}