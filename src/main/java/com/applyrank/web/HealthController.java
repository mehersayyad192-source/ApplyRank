package com.applyrank.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Used by the uptime monitor and by the website (to wake a sleeping free server). No database call, so it is always fast. */
@RestController
public class HealthController {
    private final Instant startedAt = Instant.now();

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", "ok");
        m.put("service", "applyrank-backend");
        m.put("startedAt", startedAt.toString());
        m.put("uptimeSeconds", Duration.between(startedAt, Instant.now()).getSeconds());
        return m;
    }
}
