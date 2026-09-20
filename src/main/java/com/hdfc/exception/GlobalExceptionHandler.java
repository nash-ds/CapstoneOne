package com.hdfc.exception;

import com.hdfc.utility.ApiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(
            AuthException ex) {

        log.warn(
                "Authentication error: {}",
                ex.getMessage()
        );

        ApiResponse<Void> response =
                ApiResponse.error(
                        ex.getStatus().value(),
                        ex.getMessage()
                );

        return ResponseEntity
                .status(ex.getStatus())
                .body(response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(
            UserNotFoundException ex) {

        log.warn(
                "User not found: {}",
                ex.getMessage()
        );

        ApiResponse<Void> response =
                ApiResponse.error(
                        HttpStatus.NOT_FOUND.value(),
                        ex.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitExceeded(
            RateLimitExceededException ex) {

        log.warn(
                "Rate limit exceeded: {}",
                ex.getMessage()
        );

        ApiResponse<Void> response =
                ApiResponse.error(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        ex.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(response);
    }

    @ExceptionHandler(CircuitBreakerException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitBreaker(
            CircuitBreakerException ex) {

        log.error(
                "Circuit breaker triggered: {}",
                ex.getMessage()
        );

        ApiResponse<Void> response =
                ApiResponse.error(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        ex.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternalService(
            ExternalServiceException ex) {

        log.error(
                "External service error: {}",
                ex.getMessage()
        );

        ApiResponse<Void> response =
                ApiResponse.error(
                        HttpStatus.SERVICE_UNAVAILABLE.value(),
                        ex.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
            IllegalArgumentException ex) {

        log.warn(
                "Bad request: {}",
                ex.getMessage()
        );

        ApiResponse<Void> response =
                ApiResponse.error(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage()
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
            Exception ex) {

        log.error(
                "Unexpected application error",
                ex
        );

        ApiResponse<Void> response =
                ApiResponse.error(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An unexpected error occurred"
                );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}