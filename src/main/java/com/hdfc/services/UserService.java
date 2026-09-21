package com.hdfc.services;

import com.hdfc.model.LoginRequest;
import com.hdfc.model.User;
import com.hdfc.repository.TokenRepository;
import com.hdfc.repository.UserRepository;

import java.util.*;

import org.springframework.stereotype.Service;

@Service 
public class UserService {

    // private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();
    // private List<User> users = new ArrayList<>();

    private UserRepository userRepository;
    private TokenRepository tokenRepository;
    private JwtService jwtService;

    public UserService(UserRepository userRepository, TokenRepository tokenRepository, JwtService jwtService){
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.jwtService = jwtService;
    }

    // @PostConstruct
    // public void init() {
    //     ObjectMapper mapper = new ObjectMapper();
    //     try (InputStream is = getClass().getResourceAsStream("/users.json")) {
    //         List<User> users = mapper.readValue(is, new TypeReference<List<User>>() {});
    //         userRepository.saveAll(users);
    //     } catch (IOException e) {
    //         throw new RuntimeException("Failed to load users.json", e);
    //     }
    // }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void logout(String token){
        String email = jwtService.getSubject(token);
        if(email != null){
        invalidateToken(email);
        invalidateRefreshToken(email);
        }
    }

    public User registerUser(LoginRequest loginRequest) {
        if(findByEmail(loginRequest.getEmail()) != null){
            return null;
        }
        User user = new User();
        user.setEmail(loginRequest.getEmail());
        user.setPassword(loginRequest.getPassword());
        user.setRoles("USER");
        userRepository.save(user);
        return user;
    }

    public boolean isAdmin(User user){
        if (user.getRoles().contains("ADMIN")){
            return true;
        }
        return false;
    }
        
    

    public List<User> getAllUsers() { return userRepository.findAll(); }
    public void save(User user){ userRepository.save(user); }
    public void registerToken(String email, String token) { tokenRepository.registerToken(email , token); }
    public void invalidateToken(String email) { tokenRepository.invalidateToken(email); }
    public boolean isTokenActive(String email ,String token) { return tokenRepository.isTokenActive(email ,token); }
    public void registerRefreshToken(String email ,String token) { tokenRepository.registerRefreshToken(email ,token); }
    public void invalidateRefreshToken(String email) { tokenRepository.invalidateRefreshToken(email); }
    public boolean isRefreshTokenActive(String email ,String token) { return tokenRepository.isRefreshTokenActive(email ,token); }



}
