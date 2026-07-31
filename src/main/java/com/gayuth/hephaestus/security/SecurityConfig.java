package com.gayuth.hephaestus.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless JWT security. Roles: ADMIN can analyze (upload) and delete reports;
 * VIEWER can read reports. Auth + the demo page are public.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/error", "/favicon.ico", "/static/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/traces/analyze").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reports/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reports/**").hasAnyRole("ADMIN", "VIEWER")
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(
            @Value("${app.security.admin-username:admin}") String adminUser,
            @Value("${app.security.admin-password:admin}") String adminPass,
            @Value("${app.security.viewer-username:viewer}") String viewerUser,
            @Value("${app.security.viewer-password:viewer}") String viewerPass,
            PasswordEncoder encoder) {
        UserDetails admin = User.withUsername(adminUser)
                .password(encoder.encode(adminPass)).roles("ADMIN").build();
        UserDetails viewer = User.withUsername(viewerUser)
                .password(encoder.encode(viewerPass)).roles("VIEWER").build();
        return new InMemoryUserDetailsManager(admin, viewer);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
