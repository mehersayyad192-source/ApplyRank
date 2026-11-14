package com.applyrank.repo;

import com.applyrank.model.Job;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {
    @Query("select distinct j.location from Job j where j.active = true and j.location is not null")
    List<String> activeLocations();
    List<Job> findByActiveTrueOrderByCreatedAtDesc();
    List<Job> findByActiveTrueAndTrackIgnoreCaseOrderByCreatedAtDesc(String track);
    List<Job> findByPostedByOrderByCreatedAtDesc(User postedBy);
    Page<Job> findByActiveTrue(Pageable pageable);
    Page<Job> findByActiveTrueAndTrackIgnoreCase(String track, Pageable pageable);
}
