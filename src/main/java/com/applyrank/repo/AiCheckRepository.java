package com.applyrank.repo;

import com.applyrank.model.AiCheck;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface AiCheckRepository extends JpaRepository<AiCheck, Long> {
    /** latest 20 checks of one user, for the "Recent checks" list (id breaks ties when two checks have the same time) */
    List<AiCheck> findTop20ByUserOrderByCreatedAtDescIdDesc(User user);

    /** every check of one user (used by the privacy export) */
    List<AiCheck> findByUserOrderByCreatedAtDesc(User user);

    /** how many AI-powered checks since a time (used for the hourly limit) */
    long countByUserAndUsedAiTrueAndCreatedAtAfter(User user, Instant since);

    void deleteByUser(User user);
}
