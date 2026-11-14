package com.applyrank.service;

import com.applyrank.model.AuditLog;
import com.applyrank.repo.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger("audit");
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    public void record(String actor, String action, String detail, String ip) {
        AuditLog a = new AuditLog();
        a.setActor(actor == null ? "anonymous" : actor);
        a.setAction(action);
        a.setDetail(detail == null ? null : detail.substring(0, Math.min(500, detail.length())));
        a.setIp(ip);
        repo.save(a);
        log.info("{} actor={} ip={}", action, a.getActor(), ip);
    }
}
