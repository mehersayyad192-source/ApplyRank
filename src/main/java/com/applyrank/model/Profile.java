package com.applyrank.model;

import jakarta.persistence.*;

/** One profile per user. Projects and certificates are small nested lists, kept as JSON text. */
@Entity
@Table(name = "profiles")
public class Profile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    private String headline;
    private String phone;
    private String location;
    @Column(length = 4000)
    private String about;
    private String status = "fresher";           // fresher | experienced
    private Double expYears = 0.0;
    private String company;
    private String jobRole;
    @Column(length = 1000)
    private String expDesc;
    private String degree;
    private String university;
    private String gradYear;
    private String cgpa;
    @Column(length = 1000)
    private String skills;                       // comma separated
    private String github;
    private String linkedin;
    private String selfRatings = "50,50,40,40,30"; // Frontend,Backend,UI/UX,Databases,DSA
    @Column(columnDefinition = "LONGTEXT")
    private String photo;                        // small data-URL (max ~300 KB, validated in the controller)
    @Column(columnDefinition = "LONGTEXT")
    private String projectsJson = "[]";
    @Column(columnDefinition = "LONGTEXT")
    private String certsJson = "[]";

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getHeadline() { return headline; }
    public void setHeadline(String v) { this.headline = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { this.phone = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { this.location = v; }
    public String getAbout() { return about; }
    public void setAbout(String v) { this.about = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Double getExpYears() { return expYears; }
    public void setExpYears(Double v) { this.expYears = v; }
    public String getCompany() { return company; }
    public void setCompany(String v) { this.company = v; }
    public String getJobRole() { return jobRole; }
    public void setJobRole(String v) { this.jobRole = v; }
    public String getExpDesc() { return expDesc; }
    public void setExpDesc(String v) { this.expDesc = v; }
    public String getDegree() { return degree; }
    public void setDegree(String v) { this.degree = v; }
    public String getUniversity() { return university; }
    public void setUniversity(String v) { this.university = v; }
    public String getGradYear() { return gradYear; }
    public void setGradYear(String v) { this.gradYear = v; }
    public String getCgpa() { return cgpa; }
    public void setCgpa(String v) { this.cgpa = v; }
    public String getSkills() { return skills; }
    public void setSkills(String v) { this.skills = v; }
    public String getGithub() { return github; }
    public void setGithub(String v) { this.github = v; }
    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String v) { this.linkedin = v; }
    public String getSelfRatings() { return selfRatings; }
    public void setSelfRatings(String v) { this.selfRatings = v; }
    public String getPhoto() { return photo; }
    public void setPhoto(String v) { this.photo = v; }
    public String getProjectsJson() { return projectsJson; }
    public void setProjectsJson(String v) { this.projectsJson = v; }
    public String getCertsJson() { return certsJson; }
    public void setCertsJson(String v) { this.certsJson = v; }
}
