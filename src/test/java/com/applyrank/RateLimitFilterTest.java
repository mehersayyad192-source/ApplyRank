package com.applyrank;

import com.applyrank.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {
    private int call(RateLimitFilter f, String ip) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        f.doFilter(req, res, new MockFilterChain());
        return res.getStatus();
    }

    @Test
    void blocksTheRequestAfterTheLimitButOnlyForThatIp() throws Exception {
        RateLimitFilter f = new RateLimitFilter(3);
        assertEquals(200, call(f, "1.1.1.1"));
        assertEquals(200, call(f, "1.1.1.1"));
        assertEquals(200, call(f, "1.1.1.1"));
        assertEquals(429, call(f, "1.1.1.1"));
        assertEquals(200, call(f, "2.2.2.2"));
    }
}
