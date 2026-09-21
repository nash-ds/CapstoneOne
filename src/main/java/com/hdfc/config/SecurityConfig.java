package com.hdfc.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Enable CORS with default/custom source configuration
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 2. Disable CSRF for REST APIs
            .csrf(AbstractHttpConfigurer::disable)
            
            // 3. Configure endpoint permissions
            .authorizeHttpRequests(auth -> auth
                // Allow Swagger UI & OpenAPI docs endpoints
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/**"
                ).permitAll()
                .anyRequest().authenticated()          // Secure all other endpoints
            )
            
            // 4. Make session management stateless (recommended for JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

@Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Add your frontend port(s) here (e.g. React/Vite/Angular)
        configuration.setAllowedOrigins(List.of(
            "http://localhost:3000", 
            "http://localhost:5173", 
            "http://localhost:4200"
        ));

        // Allowed HTTP methods
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allowed request headers
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));

        // Expose Authorization header so frontend can read JWT tokens from responses
        configuration.setExposedHeaders(List.of("Authorization"));

        // Allow credentials (cookies / Auth headers)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Default strength is 10 (good balance between security and CPU cost)
        return new BCryptPasswordEncoder();
    }
}