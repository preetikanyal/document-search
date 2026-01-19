package com.documentsearch.gateway.service;

import com.documentsearch.gateway.dto.AuthResponse;
import com.documentsearch.gateway.dto.LoginRequest;
import com.documentsearch.gateway.dto.SignupRequest;
import com.documentsearch.gateway.entity.User;
import com.documentsearch.gateway.repository.UserRepository;
import com.documentsearch.gateway.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user
     */
    public AuthResponse signup(SignupRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setTenantId(request.getTenantId());
        user.setRole(request.getRole().toUpperCase());
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);
        log.info("New user registered: {} for tenant: {}", user.getUsername(), user.getTenantId());

        // Generate token
        String token = jwtTokenProvider.generateToken(
                user.getUsername(),
                user.getTenantId(),
                user.getRole()
        );

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getTenantId(),
                user.getRole(),
                jwtTokenProvider.getExpirationMs()
        );
    }

    /**
     * Authenticate user and generate token
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        // Check if user is enabled
        if (!user.getEnabled()) {
            throw new RuntimeException("User account is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: {} from tenant: {}", user.getUsername(), user.getTenantId());

        // Generate token with tenantId and role
        String token = jwtTokenProvider.generateToken(
                user.getUsername(),
                user.getTenantId(),
                user.getRole()
        );

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getTenantId(),
                user.getRole(),
                jwtTokenProvider.getExpirationMs()
        );
    }
}

