package com.applyrank.repo;

import com.applyrank.model.Job;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByActiveTrueOrderByCreatedAtDesc();
    List<Job> findByActiveTrueAndTrackIgnoreCaseOrderByCreatedAtDesc(String track);
    List<Job> findByPostedByOrderByCreatedAtDesc(User postedBy);
}
