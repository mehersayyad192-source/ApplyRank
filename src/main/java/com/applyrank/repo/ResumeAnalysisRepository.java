package com.applyrank.repo;

import com.applyrank.model.ResumeAnalysis;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long> {
    Optional<ResumeAnalysis> findFirstByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
    java.util.List<ResumeAnalysis> findByUser(User user);
}
