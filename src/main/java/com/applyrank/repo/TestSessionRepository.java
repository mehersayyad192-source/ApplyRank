package com.applyrank.repo;

import com.applyrank.model.TestSession;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TestSessionRepository extends JpaRepository<TestSession, String> {
    Optional<TestSession> findByIdAndUser(String id, User user);
    void deleteByUser(User user);
}
