package com.applyrank;

import com.applyrank.service.ResumePolishService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumePolishServiceTest {
    private final ResumePolishService service = new ResumePolishService();

    private static final String RESUME = String.join("\n",
            "Asha Rao", "asha@gmail.com | +91 98765 43210 | github.com/asha",
            "Education", "B.Tech CSE 2021-2025, CGPA 8.4",
            "Skills", "Java, Spring Boot, React, MySQL",
            "Projects",
            "- Built a job portal with Spring Boot and React using 3 REST APIs",
            "- Reduced page load by 30% by adding caching to the search",
            "- Deployed the app and tested it with 500 demo users",
            "Experience", "Intern at Acme 2024",
            "- Developed 5 unit tests for each feature and reached 80% coverage",
            "More words so that the resume is long enough for the length check to pass easily. ".repeat(2));

    @Test
    void privateDetailsAreHiddenAndCounted() {
        ResumePolishService.Result r = service.polish(RESUME);
        assertEquals(1, r.redactions().get("emails"));
        assertEquals(1, r.redactions().get("phones"));
        assertEquals(1, r.redactions().get("links"));
        assertFalse(r.cleaned().contains("asha@gmail.com"));
        assertFalse(r.cleaned().contains("98765"));
        assertFalse(r.cleaned().contains("github.com"));
    }

    @Test
    void yearRangesAndCgpaAreNotTreatedAsPhoneNumbers() {
        ResumePolishService.Result r = service.polish(RESUME);
        assertTrue(r.cleaned().contains("2021-2025"));
        assertTrue(r.cleaned().contains("8.4"));
    }

    @Test
    void goodResumeScoresHigh() {
        assertTrue(service.polish(RESUME).score() >= 85);
    }

    @Test
    void weakShortResumeGetsManyFindings() {
        ResumePolishService.Result r = service.polish("I am hardworking.\nResponsible for many things.");
        assertTrue(r.score() < 40);
        assertTrue(r.findings().stream().anyMatch(f -> f.title().contains("No skills")));
        assertTrue(r.findings().stream().anyMatch(f -> f.title().contains("Weak phrase")));
        assertEquals("HIGH", r.findings().get(0).severity());      // the worst problems come first
    }
}
