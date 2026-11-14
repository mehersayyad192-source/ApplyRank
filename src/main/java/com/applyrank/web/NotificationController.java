package com.applyrank.web;

import com.applyrank.dto.Dtos.NotificationDto;
import com.applyrank.model.*;
import com.applyrank.repo.NotificationRepository;
import com.applyrank.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationRepository repo;
    private final CurrentUser current;
    private final Mapper mapper;

    public NotificationController(NotificationRepository repo, CurrentUser current, Mapper mapper) {
        this.repo = repo; this.current = current; this.mapper = mapper;
    }

    @GetMapping
    public List<NotificationDto> list() {
        return repo.findByUserOrderByCreatedAtDesc(current.get()).stream().map(mapper::notification).toList();
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread() {
        return Map.of("count", repo.countByUserAndSeenFalse(current.get()));
    }

    @PutMapping("/{id}/read")
    public NotificationDto read(@PathVariable Long id) {
        Notification n = repo.findById(id).filter(x -> x.getUser().getId().equals(current.get().getId()))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found."));
        n.setSeen(true);
        return mapper.notification(repo.save(n));
    }

    @PutMapping("/read-all")
    public Map<String, Integer> readAll() {
        List<Notification> all = repo.findByUserOrderByCreatedAtDesc(current.get());
        all.forEach(n -> n.setSeen(true));
        repo.saveAll(all);
        return Map.of("updated", all.size());
    }
}
