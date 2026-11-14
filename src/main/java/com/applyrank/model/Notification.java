package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = @Index(name = "idx_notif_user_created", columnList = "user_id, created_at"))
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String category;                      // updates | rank | jobs
    @Column(nullable = false)
    private String title;
    @Column(length = 1000)
    private String description;
    private String ctaLabel;
    private String ctaKind;                       // link | scan
    private String ctaArg;
    private boolean seen = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public String getCategory() { return category; }
    public void setCategory(String v) { this.category = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getCtaLabel() { return ctaLabel; }
    public void setCtaLabel(String v) { this.ctaLabel = v; }
    public String getCtaKind() { return ctaKind; }
    public void setCtaKind(String v) { this.ctaKind = v; }
    public String getCtaArg() { return ctaArg; }
    public void setCtaArg(String v) { this.ctaArg = v; }
    public boolean isSeen() { return seen; }
    public void setSeen(boolean v) { this.seen = v; }
    public Instant getCreatedAt() { return createdAt; }
}
