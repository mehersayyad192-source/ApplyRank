package com.applyrank.repo;

import com.applyrank.model.AuthToken;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByTokenHashAndPurpose(String tokenHash, String purpose);
    Optional<AuthToken> findFirstByUserAndPurposeOrderByCreatedAtDesc(User user, String purpose);
    void deleteByUser(User user);
}
