package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One row for every AI Lab check a user runs.
 * I only save small facts about the check (which tool, score, how long). I never save the code or the text
 * the user pasted, so there is nothing private sitting in my database.
 */
@Entity
@Table(name = "ai_checks")
public class AiCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** which tool was used: CODE_REVIEW now, SCAM and PAGE later */
    @Column(nullable = false, length = 20)
    private String tool;

    /** short label such as "java" */
    @Column(length = 40)
    private String label;

    private int inputLines;
    private int score;

    /** true if the AI was really asked (this is what the hourly quota counts) */
    private boolean usedAi;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public String getTool() { return tool; }
    public void setTool(String v) { this.tool = v; }
    public String getLabel() { return label; }
    public void setLabel(String v) { this.label = v; }
    public int getInputLines() { return inputLines; }
    public void setInputLines(int v) { this.inputLines = v; }
    public int getScore() { return score; }
    public void setScore(int v) { this.score = v; }
    public boolean isUsedAi() { return usedAi; }
    public void setUsedAi(boolean v) { this.usedAi = v; }
    public Instant getCreatedAt() { return createdAt; }
}
