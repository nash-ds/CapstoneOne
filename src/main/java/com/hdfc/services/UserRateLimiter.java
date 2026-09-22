package com.hdfc.services;

import java.time.Duration;
import java.util.function.Supplier;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import com.hdfc.utility.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

@Service
public class UserRateLimiter {
    private final RateLimiterRegistry registry;
 
    public UserRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(3)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO)
                .build();
        this.registry = RateLimiterRegistry.of(config);
    }
 
public ResponseEntity<?> hitPerUser( String username, Supplier<ResponseEntity<?>> supplier) {

    RateLimiter rateLimiter = registry.rateLimiter("loginRL_" + username);

    try {
        return RateLimiter.decorateSupplier(rateLimiter, supplier).get();
    } catch (RequestNotPermitted ex) {

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.error(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many login attempts. Please try again later."));
    }
}

    public boolean isUserLocked(String username) {
        RateLimiter rateLimiter = registry.rateLimiter("loginRL_" + username);
        
        return rateLimiter.getMetrics().getAvailablePermissions() <= 0;
    }

}