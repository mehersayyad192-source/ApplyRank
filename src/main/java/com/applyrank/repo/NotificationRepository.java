package com.applyrank.repo;

import com.applyrank.model.Notification;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    Page<Notification> findByUser(User user, Pageable pageable);
    long countByUserAndSeenFalse(User user);

    void deleteByUser(User user);
}
