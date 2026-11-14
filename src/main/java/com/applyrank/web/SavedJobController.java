package com.applyrank.web;

import com.applyrank.dto.Dtos.JobDto;
import com.applyrank.model.Job;
import com.applyrank.model.SavedJob;
import com.applyrank.model.User;
import com.applyrank.repo.JobRepository;
import com.applyrank.repo.SavedJobRepository;
import com.applyrank.service.CurrentUser;
import com.applyrank.service.Mapper;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Bookmark jobs for later. All three calls only ever touch the logged-in user's own list. */
@RestController
@RequestMapping("/api/saved-jobs")
public class SavedJobController {
    private final SavedJobRepository saved;
    private final JobRepository jobs;
    private final CurrentUser current;
    private final Mapper mapper;

    public SavedJobController(SavedJobRepository saved, JobRepository jobs, CurrentUser current, Mapper mapper) {
        this.saved = saved; this.jobs = jobs; this.current = current; this.mapper = mapper;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<JobDto> mine() {
        return saved.findByUserOrderByCreatedAtDesc(current.get()).stream()
                .map(SavedJob::getJob).filter(Job::isActive).map(mapper::job).toList();
    }

    @PutMapping("/{jobId}")
    @Transactional
    public Map<String, Object> save(@PathVariable Long jobId) {
        User u = current.get();
        Job job = jobs.findById(jobId).filter(Job::isActive)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found."));
        if (!saved.existsByUserAndJob(u, job)) {
            SavedJob s = new SavedJob();
            s.setUser(u);
            s.setJob(job);
            saved.save(s);
        }
        return Map.of("saved", true);
    }

    @DeleteMapping("/{jobId}")
    @Transactional
    public Map<String, Object> remove(@PathVariable Long jobId) {
        User u = current.get();
        jobs.findById(jobId).ifPresent(j -> saved.deleteByUserAndJob(u, j));
        return Map.of("saved", false);
    }
}
