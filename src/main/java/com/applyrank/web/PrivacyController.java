package com.applyrank.web;

import com.applyrank.dto.Dtos.*;
import com.applyrank.model.*;
import com.applyrank.repo.*;
import com.applyrank.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Privacy controls the user owns (GDPR / DPDP-Act style):
 *  - right of access / portability:  GET    /api/privacy/export   -> everything we store about me, as a JSON download
 *  - right to erasure:               DELETE /api/privacy/account  -> permanently deletes my account, data and resume files
 */
@RestController
@RequestMapping("/api/privacy")
public class PrivacyController {
    public record DeleteAccountRequest(@NotBlank String password) {}

    private final CurrentUser current;
    private final Mapper mapper;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final ProfileRepository profiles;
    private final TestResultRepository results;
    private final JobApplicationRepository apps;
    private final NotificationRepository notifications;
    private final ResumeAnalysisRepository resumes;
    private final JobRepository jobs;
    private final UserRepository users;
    private final TestSessionRepository sessions;
    private final AuthTokenRepository authTokens;
    private final SavedJobRepository savedJobs;
    private final AiCheckRepository aiChecks;

    public PrivacyController(CurrentUser current, Mapper mapper, PasswordEncoder encoder, AuditService audit,
                             ProfileRepository profiles, TestResultRepository results, JobApplicationRepository apps,
                             NotificationRepository notifications, ResumeAnalysisRepository resumes, JobRepository jobs,
                             UserRepository users, TestSessionRepository sessions, AuthTokenRepository authTokens, SavedJobRepository savedJobs, AiCheckRepository aiChecks) {
        this.aiChecks = aiChecks;
        this.sessions = sessions; this.authTokens = authTokens; this.savedJobs = savedJobs;
        this.current = current; this.mapper = mapper; this.encoder = encoder; this.audit = audit; this.profiles = profiles;
        this.results = results; this.apps = apps; this.notifications = notifications; this.resumes = resumes;
        this.jobs = jobs; this.users = users;
    }

    @GetMapping("/export")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> export(HttpServletRequest req) {
        User u = current.get();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("exportedAt", java.time.Instant.now().toString());
        out.put("account", mapper.user(u));
        out.put("profile", profiles.findByUser(u).map(p -> mapper.profile(u, p)).orElse(null));
        out.put("testResults", results.findByUserOrderByCreatedAtDesc(u).stream().map(mapper::test).toList());
        out.put("applications", apps.findByUserOrderByAppliedAtDesc(u).stream().map(mapper::application).toList());
        out.put("savedJobs", savedJobs.findByUserOrderByCreatedAtDesc(u).stream().map(s -> mapper.job(s.getJob())).toList());
        out.put("aiLabChecks", aiChecks.findByUserOrderByCreatedAtDesc(u).stream()
                .map(a -> Map.of("tool", a.getTool(), "label", String.valueOf(a.getLabel()), "score", a.getScore(), "usedAi", a.isUsedAi(), "at", a.getCreatedAt().toString()))
                .toList());
        out.put("notifications", notifications.findByUserOrderByCreatedAtDesc(u).stream().map(mapper::notification).toList());
        out.put("resumeChecks", resumes.findByUser(u).stream()
                .map(r -> Map.of("filename", r.getFilename(), "track", String.valueOf(r.getTrack()), "score", r.getScore(), "at", r.getCreatedAt().toString()))
                .toList());
        audit.record(u.getEmail(), "DATA_EXPORT", null, req.getRemoteAddr());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("my-applyrank-data.json").build().toString())
                .body(out);
    }

    @DeleteMapping("/account")
    @Transactional
    public Map<String, String> deleteAccount(@Valid @RequestBody DeleteAccountRequest r, HttpServletRequest req) {
        User u = current.get();
        if (!encoder.matches(r.password(), u.getPasswordHash()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Password is wrong - account not deleted.");

        for (ResumeAnalysis ra : resumes.findByUser(u)) {          // uploaded files go too, not only DB rows
            try { if (ra.getStoredPath() != null) Files.deleteIfExists(Path.of(ra.getStoredPath())); }
            catch (IOException ignored) { /* file already gone */ }
        }
        resumes.deleteByUser(u);
        notifications.deleteByUser(u);
        results.deleteByUser(u);
        sessions.deleteByUser(u);
        authTokens.deleteByUser(u);
        savedJobs.deleteByUser(u);
        aiChecks.deleteByUser(u);
        apps.deleteByUser(u);
        profiles.deleteByUser(u);
        List<Job> mine = jobs.findByPostedByOrderByCreatedAtDesc(u);   // recruiter: close their jobs, keep candidates' history
        mine.forEach(j -> { j.setActive(false); j.setPostedBy(null); });
        jobs.saveAll(mine);
        audit.record(u.getEmail(), "ACCOUNT_DELETED", null, req.getRemoteAddr());
        users.delete(u);
        return Map.of("status", "deleted");
    }
}
