// package com.hdfc.services;

// import java.time.Duration;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.function.Supplier;

// import org.springframework.stereotype.Service;

// import io.github.resilience4j.ratelimiter.RateLimiter;
// import io.github.resilience4j.ratelimiter.RateLimiterConfig;
// import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

// @Service
// public class UserRateLimiter {
//     private final RateLimiterRegistry registry;

//     ConcurrentHashMap<String, RateLimiter> userLimiters = new ConcurrentHashMap<>();
 
//     public UserRateLimiter(RateLimiterRegistry registry) {
//         RateLimiterConfig config = RateLimiterConfig.custom()
//                 .limitForPeriod(3)
//                 .limitRefreshPeriod(Duration.ofMinutes(1))
//                 .timeoutDuration(Duration.ZERO)
//                 .build();
//         this.registry = RateLimiterRegistry.of(config);
//     }
 
//     public <T> T hitPerUser(String username, Supplier<T> supplier){
 
//         RateLimiter rateLimiter = registry.rateLimiter("loginRL_" + username);
 
//         return RateLimiter.decorateSupplier(rateLimiter,supplier).get();
//     }
// }