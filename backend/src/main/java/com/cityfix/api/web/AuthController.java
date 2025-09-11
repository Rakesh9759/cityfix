package com.cityfix.api.web;

import com.cityfix.api.domain.AppUser;
import com.cityfix.api.security.JwtService;
import com.cityfix.api.service.AuthService;
import com.cityfix.api.web.dto.AuthDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class AuthController {

    private final AuthService auth;
    private final JwtService jwt;

    public AuthController(AuthService auth, JwtService jwt) {
        this.auth = auth;
        this.jwt = jwt;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        AppUser u = auth.registerCitizen(req.email(), req.password());
        String token = jwt.createToken(u);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        AppUser u = auth.authenticate(req.email(), req.password());
        String token = jwt.createToken(u);
        return ResponseEntity.ok(new AuthResponse(token));
    }

    @GetMapping("/api/me")
    public ResponseEntity<MeResponse> me(Authentication authn) {
        if (authn == null) return ResponseEntity.status(401).build();
        String email = authn.getName();
        String role = authn.getAuthorities().stream()
                .findFirst().map(a -> a.getAuthority()).orElse("ROLE_UNKNOWN");
        return ResponseEntity.ok(new MeResponse(email, role));
    }
}