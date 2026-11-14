package com.applyrank.service;

import com.applyrank.model.AuthToken;
import com.applyrank.model.User;
import com.applyrank.repo.AuthTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/** Creates and checks one-time tokens. The raw token only ever lives in the e-mail link; the database keeps its hash. */
@Service
public class TokenService {
    private static final SecureRandom RNG = new SecureRandom();
    private final AuthTokenRepository repo;

    public TokenService(AuthTokenRepository repo) { this.repo = repo; }

    static String hash(String raw) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public String issue(User user, String purpose, Duration ttl) {
        byte[] b = new byte[32];
        RNG.nextBytes(b);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        AuthToken t = new AuthToken();
        t.setUser(user);
        t.setPurpose(purpose);
        t.setTokenHash(hash(raw));
        t.setExpiresAt(Instant.now().plus(ttl));
        repo.save(t);
        return raw;
    }

    /** Returns the token (and marks it used) only if it exists, matches the purpose, is unused and not expired. */
    @Transactional
    public Optional<AuthToken> consume(String raw, String purpose) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        Optional<AuthToken> found = repo.findByTokenHashAndPurpose(hash(raw.trim()), purpose)
                .filter(t -> !t.isUsed() && t.getExpiresAt().isAfter(Instant.now()));
        found.ifPresent(t -> { t.setUsed(true); repo.save(t); });
        return found;
    }

    public boolean recentlyIssued(User user, String purpose, Duration within) {
        return repo.findFirstByUserAndPurposeOrderByCreatedAtDesc(user, purpose)
                .map(t -> t.getCreatedAt().isAfter(Instant.now().minus(within))).orElse(false);
    }
}
