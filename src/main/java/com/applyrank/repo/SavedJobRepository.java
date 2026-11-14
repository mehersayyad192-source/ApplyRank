package com.applyrank.repo;

import com.applyrank.model.Job;
import com.applyrank.model.SavedJob;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {
    List<SavedJob> findByUserOrderByCreatedAtDesc(User user);
    boolean existsByUserAndJob(User user, Job job);
    void deleteByUserAndJob(User user, Job job);
    void deleteByUser(User user);
}
