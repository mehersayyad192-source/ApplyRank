package com.applyrank;

import com.applyrank.service.QuestionBank;
import com.applyrank.service.ScoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScoreServiceTest {
    ScoreService scorer;
    QuestionBank bank;

    @BeforeEach
    void setUp() {
        bank = new QuestionBank(new ObjectMapper());
        bank.init();
        scorer = new ScoreService(bank);
    }

    @Test
    void perfectBackendRunGetsTopTier() {
        List<Integer> keys = bank.get("backend").mcqKeys();
        var r = scorer.score("backend", keys, List.of(1.0, 1.0), List.of(), 500, 5);
        assertEquals(40, r.mcq());
        assertEquals(35, r.code());
        assertEquals(75, r.total());               // no interview answers -> 0 of 25
        assertEquals(bank.get("backend").titles().get(2), r.rankTitle());
        assertTrue(r.badges().stream().anyMatch(b -> b.contains("Test Champion")));
    }

    @Test
    void emptySubmissionScoresZero() {
        var r = scorer.score("frontend", Arrays.asList(null, null, null, null, null), List.of(0.0, 0.0), List.of(), 0, 0);
        assertEquals(0, r.total());
        assertEquals(bank.get("frontend").titles().get(0), r.rankTitle());
        assertTrue(r.badges().isEmpty());
    }

    @Test
    void codeFractionsAreClampedSoTheBrowserCannotCheat() {
        var r = scorer.score("data", Arrays.asList(null, null, null, null, null), List.of(99.0, -5.0), List.of(), 0, 0);
        assertEquals(15, r.code());
    }

    @Test
    void tiers() {
        assertEquals(0, ScoreService.tier(39));
        assertEquals(1, ScoreService.tier(40));
        assertEquals(2, ScoreService.tier(60));
        assertEquals(3, ScoreService.tier(80));
    }

    @Test
    void shortInterviewAnswerIsCapped() {
        var q = bank.get("backend").interview().get(0);
        String all = String.join(" ", q.rubric().stream().flatMap(x -> x.words().stream()).toList());
        assertTrue(scorer.scoreInterviewAnswer(q, "rest api").fraction() <= 0.25);
        assertTrue(scorer.scoreInterviewAnswer(q, all + " plus some extra words to make it long enough").fraction() > 0.9);
    }
}
