package com.applyrank.repo;

import com.applyrank.model.Job;
import com.applyrank.model.JobApplication;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    @EntityGraph(attributePaths = {"job"})
    List<JobApplication> findByUserOrderByAppliedAtDesc(User user);
    @EntityGraph(attributePaths = {"user", "job"})      // ONE join query instead of 1 + N lookups
    List<JobApplication> findByJobOrderByAppliedAtDesc(Job job);
    boolean existsByUserAndJob(User user, Job job);

    void deleteByUser(User user);
}
