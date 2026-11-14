package com.applyrank.web;

import com.applyrank.dto.Dtos.*;
import com.applyrank.model.*;
import com.applyrank.repo.*;
import com.applyrank.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
public class TestController {
    private final QuestionBank bank;
    private final ScoreService scorer;
    private final TestResultRepository results;
    private final CurrentUser current;
    private final Mapper mapper;
    private final NotificationService notes;
    private final QuestionService questionService;

    public TestController(QuestionBank bank, ScoreService scorer, TestResultRepository results, CurrentUser current,
                          Mapper mapper, NotificationService notes, QuestionService questionService) {
        this.questionService = questionService;
        this.bank = bank; this.scorer = scorer; this.results = results; this.current = current; this.mapper = mapper; this.notes = notes;
    }

    @GetMapping("/domains")
    public List<DomainDto> domains() {
        return bank.all().entrySet().stream().map(e -> new DomainDto(e.getKey(), e.getValue().name())).toList();
    }

    /** Live feedback for one typed interview answer (the browser calls this while the candidate practises). */
    @PostMapping("/interview-score")
    public InterviewScoreResponse interview(@Valid @RequestBody InterviewScoreRequest r) {
        QuestionBank.Domain d = bank.get(r.domain());
        if (d == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown domain.");
        QuestionBank.InterviewQ q = d.interview().stream().filter(x -> x.q().equals(r.question())).findFirst()
                .orElse(d.interview().get(0));
        ScoreService.InterviewScore s = scorer.scoreInterviewAnswer(q, r.answer());
        String feedback = s.note().isEmpty()
                ? (s.missed().isEmpty() ? "Great answer - you covered every key point." : "Good. Also mention: " + String.join(", ", s.missed()) + ".")
                : s.note();
        return new InterviewScoreResponse(s.fraction(), s.covered(), s.missed(), feedback);
    }

    /** Starts an attempt: 5 RANDOM questions of the track, without the correct answers. */
    @GetMapping("/questions")
    public TestSessionDto questions(@RequestParam String domain) {
        return questionService.start(current.get(), domain);
    }

    /** Locks one answer and reveals whether it was right (+ the explanation). */
    @PostMapping("/answer")
    public AnswerResponse answer(@Valid @RequestBody AnswerRequest r) {
        return questionService.answer(current.get(), r);
    }

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    public TestResultDto submit(@Valid @RequestBody TestSubmitRequest r) {
        User u = current.get();
        if (bank.get(r.domain()) == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown domain.");
        int before = results.findFirstByUserOrderByCreatedAtDesc(u).map(t -> ScoreService.tier(t.getTotal())).orElse(-1);
        QuestionService.Graded g = questionService.close(u, r.sessionId(), r.domain());     // answers come from the SERVER session
        ScoreService.ScoreResult s = scorer.score(r.domain(), g.keys(), g.answers(), r.code(), r.interview(),
                r.timeUsed() == null ? 0 : r.timeUsed(), r.bestStreak() == null ? 0 : r.bestStreak());

        TestResult t = new TestResult();
        t.setUser(u);
        t.setDomain(r.domain().toLowerCase());
        t.setMcq(s.mcq());
        t.setCode(s.code());
        t.setInterview(s.interview());
        t.setTotal(s.total());
        t.setRankTitle(s.rankTitle());
        t.setXp(Math.max(0, Math.min(1000, r.xp() == null ? 0 : r.xp())));
        t.setTimeUsed(r.timeUsed() == null ? 0 : r.timeUsed());
        t.setBadges(Mapper.join(s.badges()));
        results.save(t);

        int after = ScoreService.tier(s.total());
        if (after > before) {
            notes.push(u, "rank", "Rank up: " + s.rankTitle() + "!", "You scored " + s.total() + "/100 in " + bank.get(r.domain()).name() + ".",
                    "View rank", "link", "dashboard.html");
        } else {
            notes.push(u, "rank", "Test verified", "You scored " + s.total() + "/100 (" + s.rankTitle() + ").", "Retake", "link", "skilltest.html");
        }
        return mapper.test(t);
    }

    @GetMapping("/latest")
    public TestResultDto latest() {
        return results.findFirstByUserOrderByCreatedAtDesc(current.get()).map(mapper::test)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No test taken yet."));
    }

    @GetMapping("/history")
    public List<TestResultDto> history() {
        return results.findByUserOrderByCreatedAtDesc(current.get()).stream().map(mapper::test).toList();
    }
}
