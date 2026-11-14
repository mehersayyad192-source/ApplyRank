package com.applyrank.repo;

import com.applyrank.model.Profile;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUser(User user);

    void deleteByUser(User user);
}
