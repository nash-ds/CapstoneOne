//package com.hdfc.filter;
//
//import java.io.IOException;
//import java.util.Collections;
//
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import com.hdfc.services.JwtService;
//import com.hdfc.services.UserService;
//
//import io.jsonwebtoken.JwtException;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
// @Component
// public class JwtAuthenticationFilter extends OncePerRequestFilter {
//
//     private final JwtService jwtService;
//     private final UserService userService;
//
//     public JwtAuthenticationFilter(JwtService jwtService, UserService userService) {
//         this.jwtService = jwtService;
//         this.userService = userService;
//     }
//
//     @Override
//     protected void doFilterInternal(HttpServletRequest request,
//                                     HttpServletResponse response,
//                                     FilterChain filterChain) throws ServletException, IOException  {
//
//         String authHeader = request.getHeader("Authorization");
//
//         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//             filterChain.doFilter(request, response);
//             return;
//         }
//
//         String token = authHeader.substring(7);
//
//         if (!userService.isTokenActive(token) || !jwtService.isValid(token)) {
//             response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//             return;
//         }
//
//         try {
//             String email = jwtService.getSubject(token);
//             UsernamePasswordAuthenticationToken authentication =
//                     new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
//
//             SecurityContextHolder.getContext().setAuthentication(authentication);
//         } catch (JwtException e) {
//             response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
//             return;
//         }
//
//         filterChain.doFilter(request, response);
//     }
// }