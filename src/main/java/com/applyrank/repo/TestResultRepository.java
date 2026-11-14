package com.applyrank.repo;

import com.applyrank.model.TestResult;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface TestResultRepository extends JpaRepository<TestResult, Long> {
    Optional<TestResult> findFirstByUserOrderByCreatedAtDesc(User user);
    List<TestResult> findByUserOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);

    /** Latest-first results of MANY users in one query (used to rank applicants without an N+1). */
    @Query("select t from TestResult t where t.user in :users order by t.createdAt desc")
    List<TestResult> findByUsers(@Param("users") java.util.Collection<User> users);
}
