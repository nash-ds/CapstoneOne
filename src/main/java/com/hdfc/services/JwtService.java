package com.hdfc.services;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.hdfc.model.User;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Service 
public class JwtService {

    private final SecretKey secretKey;

    public JwtService(){
        this.secretKey = Jwts.SIG.HS256.key().build();
    }

    public String generateToken(User user){
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId",user.getUserId())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+ 1000*60*15))
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
