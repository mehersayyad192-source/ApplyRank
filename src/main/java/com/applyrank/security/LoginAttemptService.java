package com.applyrank.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Brute-force protection: 5 wrong passwords for the same e-mail from the same IP lock that pair for 15 minutes.
 * In-memory on purpose (simple, no extra infra). For several server instances use Redis instead - see README.
 */
@Service
public class LoginAttemptService {
    public static final int MAX_FAILS = 5;
    public static final Duration LOCK = Duration.ofMinutes(15);

    private record State(int fails, Instant lockedUntil) {}
    private final ConcurrentHashMap<String, State> map = new ConcurrentHashMap<>();

    private static String key(String email, String ip) {
        return (email == null ? "" : email.trim().toLowerCase()) + "|" + ip;
    }

    public boolean isLocked(String email, String ip) {
        State s = map.get(key(email, ip));
        return s != null && s.lockedUntil() != null && s.lockedUntil().isAfter(Instant.now());
    }

    public long secondsLeft(String email, String ip) {
        State s = map.get(key(email, ip));
        return s == null || s.lockedUntil() == null ? 0 : Math.max(0, Duration.between(Instant.now(), s.lockedUntil()).getSeconds());
    }

    public void failed(String email, String ip) {
        map.merge(key(email, ip), new State(1, null), (old, ignored) -> {
            if (old.lockedUntil() != null && old.lockedUntil().isBefore(Instant.now())) return new State(1, null);   // lock expired
            int n = old.fails() + 1;
            return new State(n, n >= MAX_FAILS ? Instant.now().plus(LOCK) : null);
        });
    }

    public void succeeded(String email, String ip) {
        map.remove(key(email, ip));
    }
}
