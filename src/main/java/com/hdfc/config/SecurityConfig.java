package com.hdfc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Disable CSRF for REST APIs
            .csrf(AbstractHttpConfigurer::disable)
            
            // 2. Configure endpoint permissions
            .authorizeHttpRequests(auth -> auth
                // Allow Swagger UI & OpenAPI docs endpoints
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/login",
                    "/auth",
                    "/logoutUser",
                    "/register",
                    "/refresh"
                ).permitAll()// Allow public access to /login
                // .requestMatchers("/auth", "/logout").authenticated()
                .anyRequest().authenticated()          // Secure all other endpoints
            )
            
            // 3. Make session management stateless (recommended for JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}