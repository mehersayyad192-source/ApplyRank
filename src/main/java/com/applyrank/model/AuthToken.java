package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

/** One-time link token (password reset / e-mail verification). Only the SHA-256 HASH is stored, never the token itself. */
@Entity
@Table(name = "auth_tokens", indexes = @Index(name = "idx_auth_token_user", columnList = "user_id, purpose"))
public class AuthToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private String purpose;                 // RESET | VERIFY

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private boolean used = false;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String v) { this.tokenHash = v; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String v) { this.purpose = v; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant v) { this.expiresAt = v; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean v) { this.used = v; }
}
