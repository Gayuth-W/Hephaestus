package com.gayuth.hephaestus.controller;

import com.gayuth.hephaestus.dto.LoginRequest;
import com.gayuth.hephaestus.dto.LoginResponse;
import com.gayuth.hephaestus.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Exchanges username/password for a signed JWT. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwt;

    public AuthController(AuthenticationManager authManager, JwtService jwt) {
        this.authManager = authManager;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            List<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).toList();
            return ResponseEntity.ok(new LoginResponse(jwt.issue(auth.getName(), roles), auth.getName(), roles));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        }
    }
}
