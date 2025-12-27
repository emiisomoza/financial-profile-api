package com.finantialhub.finantialhubapi.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("users")
public class User {

    @Id
    private UUID id;

    private String email;
    private String fullName;
    private String passwordHash;
    private Instant createdAt;

    // constructors

    public User(UUID id, String email, String fullName, String passwordHash, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public static User createNew(String email, String fullName, String passwordHash) {
        return new User(
                UUID.randomUUID(),
                email,
                fullName,
                passwordHash,
                Instant.now()
        );
    }

    // getters (no setters for now, keep it immutable-ish)

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
