package com.applyrank.web;

import com.applyrank.dto.Dtos.*;
import com.applyrank.model.*;
import com.applyrank.repo.*;
import com.applyrank.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class JobController {
    private final JobRepository jobs;
    private final JobApplicationRepository apps;
    private final TestResultRepository results;
    private final CurrentUser current;
    private final Mapper mapper;
    private final NotificationService notes;

    public JobController(JobRepository jobs, JobApplicationRepository apps, TestResultRepository results,
                         CurrentUser current, Mapper mapper, NotificationService notes) {
        this.jobs = jobs; this.apps = apps; this.results = results; this.current = current; this.mapper = mapper; this.notes = notes;
    }

    @GetMapping("/jobs")
    public List<JobDto> list(@RequestParam(required = false) String track) {
        List<Job> list = (track == null || track.isBlank())
                ? jobs.findByActiveTrueOrderByCreatedAtDesc()
                : jobs.findByActiveTrueAndTrackIgnoreCaseOrderByCreatedAtDesc(track);
        return list.stream().map(mapper::job).toList();
    }

    @GetMapping("/jobs/{id}")
    public JobDto one(@PathVariable Long id) {
        return mapper.job(jobs.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found.")));
    }

    @PostMapping("/jobs/{id}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDto apply(@PathVariable Long id) {
        User u = current.get();
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
