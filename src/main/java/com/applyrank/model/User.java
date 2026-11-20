package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CANDIDATE;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    /**
     * "Log out everywhere" counter. Every login token carries this number. When the user logs out or resets the
     * password I add 1 here, so every older token (which still carries the old number) stops working at once.
     * It is Integer (not int) so old rows without a value still load.
     */
    private Integer tokenVersion = 0;

    private Boolean emailVerified = Boolean.FALSE;      // Boolean (not boolean) so old rows without a value still load

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getTokenVersion() { return tokenVersion == null ? 0 : tokenVersion; }
    public void bumpTokenVersion() { tokenVersion = getTokenVersion() + 1; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isEmailVerified() { return Boolean.TRUE.equals(emailVerified); }
    public void setEmailVerified(boolean v) { this.emailVerified = v; }
}
