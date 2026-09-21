// package com.hdfc.services;

// import org.springframework.stereotype.Service;

// import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
// import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

// @Service
// public class ResilientLoginService {

// private final MockExternalLoginService externalLoginService;

// public ResilientLoginService(
// MockExternalLoginService externalLoginService) {

// this.externalLoginService = externalLoginService;
// }

// @RateLimiter(
// name = "loginRateLimiter",
// fallbackMethod = "loginFallback"
// )
// @CircuitBreaker(
// name = "externalLoginService",
// fallbackMethod = "loginFallback"
// )
// public boolean authenticate(
// String email,
// String password,
// boolean simulateFailure) {

// return externalLoginService.authenticate(
// email,
// password,
// simulateFailure
// );
// }

// public boolean loginFallback(
// String email,
// String password,
// boolean simulateFailure,
// Throwable exception) {

// System.out.println(
// "Resilience fallback triggered: "
// + exception.getMessage()
// );

// return false;
// }
// }
