package com.cityfix.api.service;

import com.cityfix.api.domain.AppUser;
import com.cityfix.api.domain.Role;
import com.cityfix.api.repo.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public AppUser registerCitizen(String email, String rawPassword) {
        String normEmail = email == null ? null : email.toLowerCase().trim();
        if (normEmail == null || normEmail.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (users.existsByEmail(normEmail)) {
            throw new IllegalArgumentException("Email already registered");
        }

        AppUser u = new AppUser();
        u.setEmail(normEmail);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(Role.citizen);
        return users.save(u);
    }

    public AppUser authenticate(String email, String rawPassword) {
        String normEmail = email == null ? null : email.toLowerCase().trim();
        AppUser u = users.findByEmail(normEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!encoder.matches(rawPassword, u.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return u;
    }
}