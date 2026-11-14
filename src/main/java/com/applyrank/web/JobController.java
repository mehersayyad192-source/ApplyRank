package com.applyrank.web;

import com.applyrank.dto.Dtos.*;
import com.applyrank.model.*;
import com.applyrank.repo.*;
import com.applyrank.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class JobController {
    private final JobRepository jobs;
    private final JobApplicationRepository apps;
    private final TestResultRepository results;
    private final CurrentUser current;
    private final Mapper mapper;
    private final NotificationService notes;
    private final boolean requireVerified;

    public JobController(JobRepository jobs, JobApplicationRepository apps, TestResultRepository results,
                         CurrentUser current, Mapper mapper, NotificationService notes,
                         @Value("${app.auth.require-verified-email:false}") boolean requireVerified) {
        this.requireVerified = requireVerified;
        this.jobs = jobs; this.apps = apps; this.results = results; this.current = current; this.mapper = mapper; this.notes = notes;
    }

    /**
     * Job Corner search.  Every parameter is optional:
     *   q=java pune          words typed in the search box (all must match)
     *   track=backend        one track
     *   location=Pune,Delhi  one or more cities (any of them)
     *   remote=true          work-from-home jobs only
     *   maxRank=60           only jobs whose rank bar is 60 or lower ("jobs I qualify for")
     *   sort=new | rank_low | rank_high
     *   page=0&size=10       one page; total count comes back in the X-Total-Count header (without page: everything)
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<JobDto>> list(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String track,
                                             @RequestParam(required = false) List<String> location,
                                             @RequestParam(defaultValue = "false") boolean remote,
                                             @RequestParam(required = false) Integer maxRank,
                                             @RequestParam(defaultValue = "new") String sort,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(defaultValue = "10") int size) {
        Sort order = switch (sort) {
            case "rank_low" -> Sort.by(Sort.Direction.ASC, "minRank").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "rank_high" -> Sort.by(Sort.Direction.DESC, "minRank").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Specification<Job> spec = JobSearch.of(q, track, location, remote, maxRank);
        if (page == null)
            return ResponseEntity.ok(jobs.findAll(spec, order).stream().map(mapper::job).toList());
        Pageable pg = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(50, size)), order);
        Page<Job> res = jobs.findAll(spec, pg);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(res.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(res.getTotalPages()))
                .body(res.getContent().stream().map(mapper::job).toList());
    }

    /** Cities for the location filter chips (every city that has an open job, written once). */
    @GetMapping("/jobs/locations")
    public List<String> locations() {
        Map<String, String> unique = new TreeMap<>();
        for (String raw : jobs.activeLocations()) {
            for (String part : raw.split("[,/]")) {
                String c = part.trim();
                if (c.isEmpty()) continue;
                String nice = Arrays.stream(c.split("\\s+"))
                        .map(w -> w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase())
                        .collect(Collectors.joining(" "));
                unique.putIfAbsent(c.toLowerCase(), nice);
            }
        }
        return new ArrayList<>(unique.values());
    }

    @GetMapping("/jobs/{id}")
    public JobDto one(@PathVariable Long id) {
        return mapper.job(jobs.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found.")));
    }

    @PostMapping("/jobs/{id}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto apply(@PathVariable Long id) {
        User u = current.get();
        if (requireVerified && !u.isEmailVerified())
            throw new ApiException(HttpStatus.FORBIDDEN, "Please verify your email before applying. Check your inbox (or the backend console in dev mode).");
        Job job = jobs.findById(id).filter(Job::isActive).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found."));
        if (apps.existsByUserAndJob(u, job)) throw new ApiException(HttpStatus.CONFLICT, "You already applied to this job.");
        JobApplication a = new JobApplication();
        a.setUser(u);
        a.setJob(job);
        a.setRankAtApply(results.findFirstByUserOrderByCreatedAtDesc(u).map(TestResult::getTotal).orElse(null));
        apps.save(a);
        notes.push(u, "updates", "Application sent", "You applied to " + job.getTitle() + " at " + job.getCompany() + ".",
                "View status", "link", "notifications.html");
        return mapper.application(a);
    }

    @GetMapping("/applications")
    public List<ApplicationDto> mine() {
        return apps.findByUserOrderByAppliedAtDesc(current.get()).stream().map(mapper::application).toList();
    }
}
