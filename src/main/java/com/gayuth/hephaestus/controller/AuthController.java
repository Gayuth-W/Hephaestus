package com.gayuth.hephaestus.controller;

import com.gayuth.hephaestus.dto.AuthResponse;
import com.gayuth.hephaestus.dto.LoginRequest;
import com.gayuth.hephaestus.dto.RegisterRequest;
import com.gayuth.hephaestus.persistence.User;
import com.gayuth.hephaestus.persistence.UserRepository;
import com.gayuth.hephaestus.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Self-service auth: register a new account or log in, both returning a JWT.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder,
            AuthenticationManager authManager, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        if (email.isBlank() || req.password() == null || req.password().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Email is required and password must be at least 6 characters."));
        }
        if (users.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "An account with that email already exists."));
        }
        users.save(new User(email, encoder.encode(req.password())));
        return ResponseEntity.ok(new AuthResponse(jwt.issue(email), email));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String email = req.email() == null ? "" : req.email().trim().toLowerCase();
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.password()));
            return ResponseEntity.ok(new AuthResponse(jwt.issue(email), email));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password."));
        }
    }
}
