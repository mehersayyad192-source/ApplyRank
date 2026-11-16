package com.applyrank;

import com.applyrank.service.CodeReviewService.Finding;
import com.applyrank.service.ScamDetectorService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the Scam Detector rules: an obvious scam must score high, a normal offer must stay clean. */
class ScamDetectorServiceTest {
    private final ScamDetectorService detector = new ScamDetectorService();

    private boolean has(List<Finding> l, String part) {
        return l.stream().anyMatch(f -> f.title().toLowerCase().contains(part.toLowerCase()));
    }

    @Test
    void obviousScamIsHighRisk() {
        var r = detector.check("Congratulations! You are selected without interview for Data Entry job. Earn Rs 5000 per day.\n"
                + "Pay registration fee Rs 999 (refundable). Contact hr.jobs2026@gmail.com on WhatsApp. "
                + "Share your Aadhaar and bank details. Limited seats, within 24 hours!");
        assertEquals("HIGH", r.level());
        assertEquals(100, r.score());
        assertTrue(has(r.findings(), "pay money"));
        assertTrue(has(r.findings(), "bank"));
        assertTrue(has(r.findings(), "whatsapp"));
        assertTrue(has(r.findings(), "without an interview"));
        assertTrue(has(r.findings(), "free personal email"));
        assertEquals("HIGH", r.findings().get(0).severity(), "most serious sign must be listed first");
    }

    @Test
    void normalOfferStaysClean() {
        var r = detector.check("Hello Meher, thank you for interviewing with Acme Technologies Pvt Ltd for the Java Developer role in Pune. "
                + "We are pleased to share the offer details. Please review the attached letter from careers@acme.com and reply by next Friday. "
                + "Our HR team is available on the office phone for any questions about the joining process and onboarding.");
        assertEquals("LOW", r.level());
        assertTrue(r.findings().isEmpty(), r.findings().toString());
        assertEquals(0, r.score());
    }

    @Test
    void remoteControlAppsAreFlagged() {
        var r = detector.check("Please install AnyDesk so we can set up your laptop. This text is padded so that it is longer than one hundred "
                + "and fifty characters and does not trigger the very-short-message note.");
        assertTrue(has(r.findings(), "remote-control"));
    }

    @Test
    void tinyMessageGetsALowNote() {
        assertTrue(has(detector.check("hi job").findings(), "very little information"));
    }

    @Test
    void lineNumberPointsToTheSign() {
        var r = detector.check("Line one is normal and fairly long so it is not short, written to fill space nicely for the test.\nPay the registration fee today please.");
        var f = r.findings().stream().filter(x -> x.title().contains("pay money")).findFirst().orElseThrow();
        assertEquals(2, f.line());
    }
}
