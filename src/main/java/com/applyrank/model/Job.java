package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "jobs", indexes = @Index(name = "idx_job_track_active", columnList = "track, active"))
public class Job {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String company;
    private String location;
    private String track;                         // frontend | backend | data | fullstack
    private String ctc;
    @Column(length = 4000)
    private String description;
    @Column(length = 1000)
    private String requirements;                  // comma separated skills
    private int minRank = 0;                      // typical verified-rank bar for this role
    private boolean active = true;

    @ManyToOne
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getCompany() { return company; }
    public void setCompany(String v) { this.company = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }
    public String getTrack() { return track; }
    public void setTrack(String v) { this.track = v; }
    public String getCtc() { return ctc; }
    public void setCtc(String v) { this.ctc = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String v) { this.requirements = v; }
    public int getMinRank() { return minRank; }
    public void setMinRank(int v) { this.minRank = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
    public User getPostedBy() { return postedBy; }
    public void setPostedBy(User v) { this.postedBy = v; }
    public Instant getCreatedAt() { return createdAt; }
}
