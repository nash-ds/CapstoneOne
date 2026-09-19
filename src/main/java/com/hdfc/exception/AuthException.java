package com.hdfc.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final HttpStatus status;

    private AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }


    // =========================================================
    // LOGIN EXCEPTIONS
    // =========================================================

    public static AuthException emailRequired() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "Email ID is required"
        );
    }

    public static AuthException passwordRequired() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "Password is required"
        );
    }

    public static AuthException emailNotRegistered() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "Email ID is not registered"
        );
    }

    public static AuthException incorrectPassword() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "Incorrect password"
        );
    }


    // =========================================================
    // REGISTER EXCEPTIONS
    // =========================================================

    public static AuthException emailAlreadyRegistered() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "Email ID is already registered"
        );
    }


    // =========================================================
    // AUTH EXCEPTIONS
    // =========================================================

    public static AuthException authorizationTokenRequired() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "Authorization token is required"
        );
    }

    public static AuthException authorizationTokenEmpty() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "Authorization token is empty"
        );
    }

    public static AuthException tokenInactive() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "Token has been logged out or is inactive"
        );
    }

    public static AuthException tokenExpired() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "Token has expired"
        );
    }

    public static AuthException invalidAuthenticationToken() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "Invalid authentication token"
        );
    }


    // =========================================================
    // REFRESH TOKEN EXCEPTIONS
    // =========================================================

    public static AuthException refreshTokenRequired() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "Refresh token is required"
        );
    }

    public static AuthException refreshTokenEmpty() {
        return new AuthException(
                HttpStatus.BAD_REQUEST,
                "Refresh token is empty"
        );
    }

    public static AuthException refreshTokenInactive() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token is inactive or revoked"
        );
    }

    public static AuthException refreshTokenExpired() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "Refresh token has expired"
        );
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "Invalid refresh token"
        );
    }

    public static AuthException refreshUserNotFound() {
        return new AuthException(
                HttpStatus.NOT_FOUND,
                "User associated with refresh token was not found"
        );
    }


    // =========================================================
    // LOGOUT EXCEPTIONS
    // =========================================================

    public static AuthException logoutTokenInactive() {
        return new AuthException(
                HttpStatus.UNAUTHORIZED,
                "Token is already inactive or invalid"
        );
    }
}