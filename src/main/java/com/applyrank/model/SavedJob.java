package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

/** A job the candidate bookmarked ("saved jobs"). One row per user + job. */
@Entity
@Table(name = "saved_jobs", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_id"}))
public class SavedJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User v) { this.user = v; }
    public Job getJob() { return job; }
    public void setJob(Job v) { this.job = v; }
    public Instant getCreatedAt() { return createdAt; }
}
