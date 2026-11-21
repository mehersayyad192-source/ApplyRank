package com.applyrank.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Safety net for the live (production) server.
 * If the app runs in production mode but the JWT secret is still the demo text (or too short),
 * anyone could forge login tokens. So I stop the server from starting at all, instead of running unsafe.
 */
@Component
public class StartupChecks {
    private static final Logger log = LoggerFactory.getLogger(StartupChecks.class);
    /** the fallback written in application.properties (only for local demo) */
    static final String DEMO_SECRET = "change-this-secret-before-deploying-applyrank-2026-xyz";

    public StartupChecks(@Value("${app.production:false}") boolean production,
                         @Value("${app.jwt.secret}") String jwtSecret) {
        boolean weak = jwtSecret == null || jwtSecret.length() < 32 || jwtSecret.equals(DEMO_SECRET)
                || jwtSecret.startsWith("dev-only");
        if (production && weak) {
            throw new IllegalStateException("JWT_SECRET is missing, too short or still the demo value. "
                    + "Set a random JWT_SECRET (32+ characters) before running in production.");
        }
        if (!production && weak) {
            log.warn("Running with the demo JWT secret. This is fine on your own computer, never on a live server.");
        }
        log.info("Startup checks passed (production mode: {})", production);
    }
}
