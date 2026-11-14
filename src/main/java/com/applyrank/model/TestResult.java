package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

/** A skill-test attempt, scored on the SERVER (so the verified rank can't be faked from the browser). */
@Entity
@Table(name = "test_results", indexes = @Index(name = "idx_result_user_created", columnList = "user_id, created_at"))
public class TestResult {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String domain;
    private int mcq;
    private int code;
    private int interview;
    private int total;
    private String rankTitle;
    private int xp;
    private int timeUsed;
    private String badges;                        // comma separated

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public String getDomain() { return domain; }
    public void setDomain(String v) { this.domain = v; }
    public int getMcq() { return mcq; }
    public void setMcq(int v) { this.mcq = v; }
    public int getCode() { return code; }
    public void setCode(int v) { this.code = v; }
    public int getInterview() { return interview; }
    public void setInterview(int v) { this.interview = v; }
    public int getTotal() { return total; }
    public void setTotal(int v) { this.total = v; }
    public String getRankTitle() { return rankTitle; }
    public void setRankTitle(String v) { this.rankTitle = v; }
    public int getXp() { return xp; }
    public void setXp(int v) { this.xp = v; }
    public int getTimeUsed() { return timeUsed; }
    public void setTimeUsed(int v) { this.timeUsed = v; }
    public String getBadges() { return badges; }
    public void setBadges(String v) { this.badges = v; }
    public Instant getCreatedAt() { return createdAt; }
}
