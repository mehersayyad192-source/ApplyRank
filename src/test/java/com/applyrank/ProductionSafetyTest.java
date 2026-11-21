package com.applyrank;

import com.applyrank.config.StartupChecks;
import com.applyrank.security.RequestLogFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/** Plain unit tests (no Spring context), so they run in a fraction of a second. */
class ProductionSafetyTest {
    private static final String DEMO = "change-this-secret-before-deploying-applyrank-2026-xyz";
    private static final String STRONG = "k8Qz1vPz7mN4xT2uR9wE6yB3cH5jL0aS8dF1gV7n";

    @Test
    void productionRefusesDemoSecret() {
        assertThrows(IllegalStateException.class, () -> new StartupChecks(true, DEMO));
    }

    @Test
    void productionRefusesShortSecret() {
        assertThrows(IllegalStateException.class, () -> new StartupChecks(true, "short"));
    }

    @Test
    void productionAcceptsStrongSecret() {
        assertDoesNotThrow(() -> new StartupChecks(true, STRONG));
    }

    @Test
    void localRunAllowsDemoSecret() {
        assertDoesNotThrow(() -> new StartupChecks(false, DEMO));
    }

    @Test
    void apiRequestGetsARequestId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        MockHttpServletResponse res = new MockHttpServletResponse();
        new RequestLogFilter().doFilter(req, res, new MockFilterChain());
        assertNotNull(res.getHeader("X-Request-Id"));
        assertEquals(8, res.getHeader("X-Request-Id").length());
    }

    @Test
    void healthCheckIsNotLogged() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse res = new MockHttpServletResponse();
        new RequestLogFilter().doFilter(req, res, new MockFilterChain());
        assertNull(res.getHeader("X-Request-Id"));
    }
}
