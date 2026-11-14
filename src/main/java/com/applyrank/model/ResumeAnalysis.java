package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "resume_analyses")
public class ResumeAnalysis {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private String filename;
    private String storedPath;
    private String track;
    private int score;
    @Column(length = 2000)
    private String matched;
    @Column(length = 2000)
    private String missing;
    @Column(length = 2000)
    private String tips;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public String getFilename() { return filename; }
    public void setFilename(String v) { this.filename = v; }
    public String getStoredPath() { return storedPath; }
    public void setStoredPath(String v) { this.storedPath = v; }
    public String getTrack() { return track; }
    public void setTrack(String v) { this.track = v; }
    public int getScore() { return score; }
    public void setScore(int v) { this.score = v; }
    public String getMatched() { return matched; }
    public void setMatched(String v) { this.matched = v; }
    public String getMissing() { return missing; }
    public void setMissing(String v) { this.missing = v; }
    public String getTips() { return tips; }
    public void setTips(String v) { this.tips = v; }
    public Instant getCreatedAt() { return createdAt; }
}
