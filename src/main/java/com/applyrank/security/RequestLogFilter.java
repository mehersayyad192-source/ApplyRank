package com.applyrank.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * One log line per API request: method, path, status and how long it took.
 * Every request also gets a short id (header X-Request-Id), so a user's problem can be found in the logs.
 * Privacy: I do NOT log the query string, the body, the login token or the IP address.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLogFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestLogFilter.class);
    private static final long SLOW_MS = 2000;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String uri = req.getRequestURI();
        // the health check is called every few minutes by the uptime monitor, so it would only fill the log
        return !uri.startsWith("/api/") || uri.equals("/api/health") || "OPTIONS".equalsIgnoreCase(req.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString().substring(0, 8);
        res.setHeader("X-Request-Id", id);
        MDC.put("rid", id);
        long start = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000L;
            int status = res.getStatus();
            if (ms >= SLOW_MS) log.warn("SLOW {} {} -> {} in {} ms", req.getMethod(), req.getRequestURI(), status, ms);
            else log.info("{} {} -> {} in {} ms", req.getMethod(), req.getRequestURI(), status, ms);
            MDC.remove("rid");
        }
    }
}
