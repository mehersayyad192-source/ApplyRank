package com.applyrank;

import com.applyrank.service.CodeReviewService.Finding;
import com.applyrank.service.PageAuditService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the Page Auditor (it uses Jsoup to read the pasted HTML). */
class PageAuditServiceTest {
    private final PageAuditService auditor = new PageAuditService();

    private boolean has(List<Finding> l, String part) {
        return l.stream().anyMatch(f -> f.title().toLowerCase().contains(part.toLowerCase()));
    }

    @Test
    void badPageIsFullOfFindings() {
        var r = auditor.audit("<html><body><div><img src='a.png'><a href='#' target='_blank'>click here</a><a href='x'></a>"
                + "<input type='text' id='n'><button></button><h2>Hi</h2><h4>x</h4><font>old</font></div></body></html>");
        for (String part : List.of("DOCTYPE", "language", "Missing <title>", "viewport", "meta description", "No <h1>", "Heading levels skip",
                "without alt", "no readable text", "click here", "noopener", "without a label", "button(s) with no text", "Old HTML", "Portfolio has no link"))
            assertTrue(has(r.findings(), part), "missing finding: " + part);
        assertTrue(r.score() < 30, "got " + r.score());
        assertEquals("HIGH", r.findings().get(0).severity());
    }

    @Test
    void goodPageScoresFull() {
        var r = auditor.audit("<!DOCTYPE html><html lang='en'><head><meta charset='utf-8'><title>Meher Sayyad - Java Developer</title>"
                + "<meta name='description' content='Portfolio of Meher'><meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<meta property='og:title' content='x'><link rel='icon' href='f.ico'></head><body><header><nav>"
                + "<a href='https://github.com/m'>GitHub</a> <a href='https://www.linkedin.com/in/m'>LinkedIn</a> <a href='mailto:m@x.com'>Email me</a>"
                + "</nav></header><main><h1>Meher</h1><h2>Projects</h2><img src='a.png' alt='App' width='10' height='10'>"
                + "<label for='n'>Name</label><input id='n' type='text'></main><footer>2026</footer></body></html>");
        assertEquals(100, r.score(), r.findings().toString());
        assertTrue(r.summary().contains("Meher Sayyad"));
    }

    @Test
    void strangeIdsDoNotCrashTheAudit() {
        assertDoesNotThrow(() -> auditor.audit("<input id='a\"b]'><label for='a\"b]'>x</label>"));
    }

    @Test
    void scriptsInThePastedPageAreNeverRun() {
        // Jsoup only reads text, so this just becomes data; the test proves the audit finishes normally
        var r = auditor.audit("<html><head><script>throw new Error('boom')</script></head><body>x</body></html>");
        assertTrue(r.score() <= 100);
    }
}
