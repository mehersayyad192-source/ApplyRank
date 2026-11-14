package com.applyrank;

import com.applyrank.service.ResumeAnalyzer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResumeAnalyzerTest {
    final ResumeAnalyzer analyzer = new ResumeAnalyzer();

    @Test
    void strongResumeScoresHigherThanEmpty() {
        String good = "Education B.Tech. Skills Java Spring SQL REST API Docker Git JPA testing security. Projects: built an API serving 10k users, "
                + "cut latency by 40%. github.com/me " + "word ".repeat(200);
        var a = analyzer.analyze(good, "backend");
        var b = analyzer.analyze("hello", "backend");
        assertTrue(a.score() > 70);
        assertTrue(a.score() > b.score());
        assertFalse(b.missing().isEmpty());
    }

    @Test
    void rejectsNonPdfOrDocx() {
        assertThrows(IllegalArgumentException.class, () -> analyzer.extractText("this is just text, not a pdf".getBytes(), "x.txt"));
    }
}
