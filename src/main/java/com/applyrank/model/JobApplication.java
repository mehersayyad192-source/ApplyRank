package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * One candidate applying to one job.
 * stage: 0 Applied, 1 Resume check, 2 Rank check, 3 Skill & interview, 4 Final decision.
 */
@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_id"}),   // also serves "my applications" lookups (user_id prefix)
        indexes = @Index(name = "idx_app_job", columnList = "job_id"))               // recruiter: "all applicants of this job"
public class JobApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    private int stage = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Outcome outcome = Outcome.IN_REVIEW;

    private Integer rankAtApply;
    private String interviewOn;
    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private Instant appliedAt = Instant.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public Job getJob() { return job; }
    public void setJob(Job v) { this.job = v; }
    public int getStage() { return stage; }
    public void setStage(int v) { this.stage = v; }
    public Outcome getOutcome() { return outcome; }
    public void setOutcome(Outcome v) { this.outcome = v; }
    public Integer getRankAtApply() { return rankAtApply; }
    public void setRankAtApply(Integer v) { this.rankAtApply = v; }
    public String getInterviewOn() { return interviewOn; }
    public void setInterviewOn(String v) { this.interviewOn = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
    public Instant getAppliedAt() { return appliedAt; }
}
