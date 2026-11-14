package com.applyrank.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Security audit trail: WHO did WHAT and WHEN. Never stores passwords, tokens or message contents. */
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String actor;          // e-mail (or "anonymous")
    @Column(nullable = false)
    private String action;         // LOGIN_OK, LOGIN_FAIL, LOGIN_LOCKED, REGISTER, DATA_EXPORT, ACCOUNT_DELETED, APPLICATION_STATUS ...
    @Column(length = 500)
    private String detail;
    private String ip;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public String getActor() { return actor; }
    public void setActor(String v) { this.actor = v; }
    public String getAction() { return action; }
    public void setAction(String v) { this.action = v; }
    public String getDetail() { return detail; }
    public void setDetail(String v) { this.detail = v; }
    public String getIp() { return ip; }
    public void setIp(String v) { this.ip = v; }
    public Instant getCreatedAt() { return createdAt; }
}
