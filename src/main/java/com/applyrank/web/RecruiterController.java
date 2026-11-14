package com.applyrank.web;

import com.applyrank.dto.Dtos.*;
import com.applyrank.model.*;
import com.applyrank.repo.*;
import com.applyrank.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Recruiter-only endpoints (the security config requires the RECRUITER role for /api/recruiter/**). */
@RestController
@RequestMapping("/api/recruiter")
public class RecruiterController {
    private final JobRepository jobs;
    private final JobApplicationRepository apps;
    private final CurrentUser current;
    private final Mapper mapper;
    private final NotificationService notes;
    private final AuditService audit;
    private final TestResultRepository results;
    private final ProfileRepository profiles;

    public RecruiterController(JobRepository jobs, JobApplicationRepository apps, CurrentUser current, Mapper mapper, NotificationService notes, AuditService audit,
                               TestResultRepository results, ProfileRepository profiles) {
        this.profiles = profiles;
        this.audit = audit; this.results = results;
        this.jobs = jobs; this.apps = apps; this.current = current; this.mapper = mapper; this.notes = notes;
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public JobDto create(@Valid @RequestBody JobRequest r) {
        Job j = new Job();
        j.setTitle(r.title().trim());
        j.setCompany(r.company().trim());
        j.setLocation(r.location());
        j.setTrack(r.track() == null ? "fullstack" : r.track().toLowerCase());
        j.setCtc(r.ctc());
        j.setDescription(r.description());
        j.setRequirements(Mapper.join(r.requirements()));
        j.setMinRank(r.minRank() == null ? 0 : Math.max(0, Math.min(100, r.minRank())));
        j.setPostedBy(current.get());
        return mapper.job(jobs.save(j));
    }

    @GetMapping("/jobs")
    public List<JobDto> mine() {
        return jobs.findByPostedByOrderByCreatedAtDesc(current.get()).stream().map(mapper::job).toList();
    }

    /** Applicants of one of MY jobs, best verified rank first. */
    @GetMapping("/jobs/{id}/applicants")
    @Transactional(readOnly = true)
    public List<ApplicantDto> applicants(@PathVariable Long id) {
        Job job = ownJob(id);
        List<JobApplication> list = apps.findByJobOrderByAppliedAtDesc(job);                    // 1 query (join fetch)
        java.util.Map<Long, TestResult> latest = new java.util.HashMap<>();
        if (!list.isEmpty()) {
            var users = list.stream().map(JobApplication::getUser).distinct().toList();
            for (TestResult t : results.findByUsers(users))                                   // 1 query for ALL applicants
                latest.putIfAbsent(t.getUser().getId(), t);                                    // newest first -> first wins
        }
        return list.stream().map(a -> mapper.applicant(a, latest.get(a.getUser().getId())))
                .sorted(Comparator.comparing((ApplicantDto a) -> a.verifiedRank() == null ? -1 : a.verifiedRank()).reversed())
                .toList();
    }

    @PutMapping("/applications/{id}/status")
    @Transactional
    public ApplicationDto status(@PathVariable Long id, @RequestBody StatusUpdateRequest r) {
        JobApplication a = apps.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Application not found."));
        ownJob(a.getJob().getId());
        if (r.stage() != null) {
            if (r.stage() < 0 || r.stage() > 4) throw new ApiException(HttpStatus.BAD_REQUEST, "Stage must be 0-4.");
            a.setStage(r.stage());
        }
        if (r.outcome() != null) {
            try { a.setOutcome(Outcome.valueOf(r.outcome().toUpperCase())); }
            catch (IllegalArgumentException e) { throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown outcome: " + r.outcome()); }
        }
        if (r.interviewOn() != null) a.setInterviewOn(r.interviewOn());
        if (r.note() != null) a.setNote(r.note());
        apps.save(a);
        audit.record(current.get().getEmail(), "APPLICATION_STATUS", "application=" + a.getId() + " stage=" + a.getStage() + " outcome=" + a.getOutcome(), null);

        Job j = a.getJob();
        String what = switch (a.getOutcome()) {
            case SHORTLISTED -> "You are shortlisted for " + j.getTitle() + " at " + j.getCompany() + "!";
            case HIRED -> "Congratulations! " + j.getCompany() + " selected you for " + j.getTitle() + ".";
            case CLOSED -> "Your application for " + j.getTitle() + " at " + j.getCompany() + " was closed." + feedbackFor(a);
            default -> "Your application for " + j.getTitle() + " moved to: " + Mapper.STAGES[a.getStage()] + ".";
        };
        notes.push(a.getUser(), "updates", "Application update", what, "View status", "link", "notifications.html");
        return mapper.application(a);
    }

    /** "Why not selected": tells the candidate what to improve (rank gap + skills the job asked for that are missing in the profile). */
    private String feedbackFor(JobApplication a) {
        Job j = a.getJob();
        List<String> tips = new ArrayList<>();
        Integer rank = a.getRankAtApply();
        if (rank == null) tips.add("take the verified skill test so recruiters can see your rank");
        else if (rank < j.getMinRank()) tips.add("your rank was " + rank + " and this role usually needs " + j.getMinRank() + " (" + (j.getMinRank() - rank) + " points more)");
        String have = profiles.findByUser(a.getUser()).map(Profile::getSkills).orElse("");
        Set<String> mine = Arrays.stream(have.split(",")).map(x -> x.trim().toLowerCase()).filter(x -> !x.isEmpty()).collect(Collectors.toSet());
        String req = j.getRequirements() == null ? "" : j.getRequirements();
        List<String> missing = Arrays.stream(req.split(",")).map(String::trim).filter(x -> !x.isEmpty())
                .filter(x -> !mine.contains(x.toLowerCase())).toList();
        if (!missing.isEmpty()) tips.add("skills asked in the job but not in your profile: " + String.join(", ", missing));
        return tips.isEmpty() ? "" : " Feedback to grow: " + String.join("; ", tips) + ".";
    }

    private Job ownJob(Long id) {
        Job j = jobs.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found."));
        if (j.getPostedBy() == null || !j.getPostedBy().getId().equals(current.get().getId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "This job belongs to another recruiter.");
        return j;
    }
}
