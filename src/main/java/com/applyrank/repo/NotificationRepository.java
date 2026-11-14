package com.applyrank.repo;

import com.applyrank.model.Notification;
import com.applyrank.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    long countByUserAndSeenFalse(User user);

    void deleteByUser(User user);
}
