package com.gayuth.hephaestus.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/** An application user. Password is stored as a BCrypt hash. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected User() { } // JPA

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public Instant getCreatedAt() { return createdAt; }
}
