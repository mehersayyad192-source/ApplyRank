package com.applyrank.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple protection against a script hammering the API: at most N requests per minute per IP address (fixed window).
 * In-memory, so with several server copies you would move this to Redis (a good interview talking point).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final int limit;
    private final Map<String, long[]> hits = new ConcurrentHashMap<>();     // ip -> {minute, count}

    public RateLimitFilter(@Value("${app.rate-limit.per-minute:300}") int limit) { this.limit = limit; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !req.getRequestURI().startsWith("/api/") || "OPTIONS".equalsIgnoreCase(req.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        final long minute = System.currentTimeMillis() / 60_000L;
        long[] w = hits.compute(req.getRemoteAddr(),
                (ip, old) -> (old == null || old[0] != minute) ? new long[]{minute, 1} : new long[]{minute, old[1] + 1});
        if (hits.size() > 20_000) hits.entrySet().removeIf(e -> e.getValue()[0] != minute);     // keep memory small
        if (w[1] > limit) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Too many requests. Please wait a minute and try again.\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
