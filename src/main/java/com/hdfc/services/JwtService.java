package com.hdfc.services;

import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.stereotype.Service;

import com.hdfc.model.User;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Service 
public class JwtService {

    private final SecretKey secretKey;

    // 15 minutes for access token
    private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 5;

    // 7 days for refresh token
    private static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 20;

    public JwtService(){
        this.secretKey = Jwts.SIG.HS256.key().build();
    }

    public String generateToken(User user){
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("email",user.getEmail())
                .claim("roles",user.getRoles())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+ 1000*60*5))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user){
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("tokenType", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(secretKey)
                .compact();
    }

    public String getSubject(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String isValidDetailed(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return "VALID";
        } catch (ExpiredJwtException e) {
            // Token was validly signed, but has passed its expiration time
            return "EXPIRED";
        } catch (JwtException | IllegalArgumentException e) {
            // Malformed token, invalid signature, empty, or untrusted payload
            return "INVALID";
        }
    }

    public boolean isValid(String token){
        try{ Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

            return true;
    }
        catch (JwtException e){
            return false;
        }
    }
}
