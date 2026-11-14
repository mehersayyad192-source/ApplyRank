package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One started test attempt: WHICH questions were served and WHAT the candidate answered (stored at answer time).
 * Grading uses this row, never the browser's claims. @Version = optimistic lock, so a double submit can't count twice.
 */
@Entity
@Table(name = "test_sessions", indexes = @Index(name = "idx_session_user", columnList = "user_id"))
public class TestSession {
    @Id
    private String id;                               // random UUID, not guessable
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(nullable = false)
    private String domain;
    @Column(nullable = false, length = 200)
    private String questionIds;                      // "12,3,27,9,15" (order served)
    @Column(nullable = false, length = 200)
    private String answers;                          // "2,-,0,-,1"  (- = not answered yet)
    private boolean submitted = false;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Version
    private Long version;

    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public String getDomain() { return domain; }
    public void setDomain(String v) { this.domain = v; }
    public String getQuestionIds() { return questionIds; }
    public void setQuestionIds(String v) { this.questionIds = v; }
    public String getAnswers() { return answers; }
    public void setAnswers(String v) { this.answers = v; }
    public boolean isSubmitted() { return submitted; }
    public void setSubmitted(boolean v) { this.submitted = v; }
    public Instant getCreatedAt() { return createdAt; }
}
