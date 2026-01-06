package com.finantialhub.finantialhubapi.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("users")
public class User implements Persistable<UUID> {

    @Id
    private UUID id;

    private String email;

    @Column("full_name")
    private String fullName;

    @Column("password_hash")
    private String passwordHash;

    @Column("created_at")
    private Instant createdAt;

    @Column("role")
    private Role role;

    @Transient
    private boolean isNew;

    // === 1) No-arg constructor for Spring Data JDBC ===
    protected User() {
        // for framework use only
    }

    // === 2) Constructor used when creating new users
    private User(String email, String fullName, String passwordHash, Role role) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.createdAt = Instant.now();
        this.role = role;
        this.isNew = true;   // Tells Spring Data JDBC to INSERT
    }

    // Static factory method used in your service
    public static User createNew(String email, String fullName, String passwordHash) {
        return new User(email, fullName, passwordHash, Role.MEMBER);
    }

    // === 3) Constructor used by Spring Data JDBC when loading from DB ===
    @PersistenceCreator
    public User(UUID id,
                String email,
                String fullName,
                String passwordHash,
                Instant createdAt,
                Role role) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.role = role;
        this.isNew = false;  // Loaded from DB → UPDATE, not INSERT
    }

    // Persistable implementation ↓↓↓
    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }

    // Getters

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

    public Role getRole() {
        return role;
    }

    public User withUpdatedProfile(String email, String fullName) {
        return new User(
                this.id,
                email,
                fullName,
                this.passwordHash,
                this.createdAt,
                this.role
        );
    }
}
