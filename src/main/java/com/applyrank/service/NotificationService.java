package com.applyrank.service;

import com.applyrank.model.Notification;
import com.applyrank.model.User;
import com.applyrank.repo.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    public Notification push(User user, String category, String title, String description,
                             String ctaLabel, String ctaKind, String ctaArg) {
        Notification n = new Notification();
        n.setUser(user);
        n.setCategory(category);
        n.setTitle(title);
        n.setDescription(description);
        n.setCtaLabel(ctaLabel);
        n.setCtaKind(ctaKind);
        n.setCtaArg(ctaArg);
        return repo.save(n);
    }
}
