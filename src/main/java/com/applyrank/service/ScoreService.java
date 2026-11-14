package com.applyrank.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The scoring rules of the ApplyRank skill test (total 100):
 *   MCQ 5 x 8 = 40, Code 15 + 20 = 35, Interview 25.
 * MCQ and interview are graded here. Code results arrive as "fraction of tests passed" from the browser
 * sandbox (see README: next step = run code on the server with Judge0).
 */
@Service
public class ScoreService {
    public static final int MCQ_EACH = 8;
    public static final int[] CODE_POINTS = {15, 20};
    public static final int INTERVIEW_TOTAL = 25;

    public record InterviewScore(double fraction, List<String> covered, List<String> missed, String note) {}
    public record ScoreResult(int mcq, int code, int interview, int total, String rankTitle, List<String> badges) {}

    private final QuestionBank bank;

    public ScoreService(QuestionBank bank) {
        this.bank = bank;
    }

    /** Grades one typed interview answer against the rubric keywords (words must start at a word boundary). */
    public InterviewScore scoreInterviewAnswer(QuestionBank.InterviewQ q, String answer) {
        String text = answer == null ? "" : answer.toLowerCase();
        List<String> covered = new ArrayList<>();
        List<String> missed = new ArrayList<>();
        for (QuestionBank.Rubric r : q.rubric()) {
            boolean hit = r.words().stream().anyMatch(w -> Pattern.compile("\\b" + Pattern.quote(w.toLowerCase())).matcher(text).find());
            (hit ? covered : missed).add(r.label());
        }
        double frac = q.rubric().isEmpty() ? 0 : (double) covered.size() / q.rubric().size();
        int words = text.isBlank() ? 0 : text.trim().split("\\s+").length;
        String note = "";
        if (words < 8) {
            frac = Math.min(frac, 0.25);
            note = "That was very short - try 2-3 full sentences with a concrete example.";
        }
        return new InterviewScore(frac, covered, missed, note);
    }

    /** Grades against the fixed answer key of questions.json (used by unit tests / legacy). */
    public ScoreResult score(String domainKey, List<Integer> mcqAnswers, List<Double> codeFractions,
                             List<String> interviewAnswers, int timeUsed, int bestStreak) {
        QuestionBank.Domain d = bank.get(domainKey);
        if (d == null) throw new IllegalArgumentException("Unknown domain: " + domainKey);
        return score(domainKey, d.mcqKeys(), mcqAnswers, codeFractions, interviewAnswers, timeUsed, bestStreak);
    }

    /** Grades against the correct answers of the questions that were actually served in this session. */
    public ScoreResult score(String domainKey, List<Integer> mcqKeys, List<Integer> mcqAnswers, List<Double> codeFractions,
                             List<String> interviewAnswers, int timeUsed, int bestStreak) {
        QuestionBank.Domain d = bank.get(domainKey);
        if (d == null) throw new IllegalArgumentException("Unknown domain: " + domainKey);

        // --- MCQ: compared with the server's own answer key ---
        int correct = 0, answered = 0;
        for (int i = 0; i < mcqKeys.size(); i++) {
            Integer chosen = i < mcqAnswers.size() ? mcqAnswers.get(i) : null;
            if (chosen != null) answered++;
            if (chosen != null && chosen.equals(mcqKeys.get(i))) correct++;
        }
        int mcq = correct * MCQ_EACH;

        // --- Code: clamp every fraction to 0..1 ---
        List<Double> fracs = new ArrayList<>();
        double codeSum = 0;
        for (int i = 0; i < CODE_POINTS.length; i++) {
            double f = i < codeFractions.size() && codeFractions.get(i) != null ? codeFractions.get(i) : 0;
            f = Math.max(0, Math.min(1, f));
            fracs.add(f);
            codeSum += f * CODE_POINTS[i];
        }
        int code = (int) Math.round(codeSum);

        // --- Interview: graded here from the typed answers ---
        double ivSum = 0;
        int ivAnswered = 0;
        for (int i = 0; i < d.interview().size(); i++) {
            String a = interviewAnswers != null && i < interviewAnswers.size() ? interviewAnswers.get(i) : null;
            if (a != null && !a.isBlank()) {
                ivAnswered++;
                ivSum += scoreInterviewAnswer(d.interview().get(i), a).fraction();
            }
        }
        double ivAvg = d.interview().isEmpty() ? 0 : ivSum / d.interview().size();
        int interview = (int) Math.round(ivAvg * INTERVIEW_TOTAL);

        int total = Math.min(100, mcq + code + interview);

        // --- Rank title ---
        String rank = d.titles().get(tier(total));

        // --- Badges ---
        List<String> badges = new ArrayList<>();
        if (bestStreak >= 3) badges.add("🔥 Streak Master");
        if (fracs.get(0) == 1.0) badges.add("🐞 Bug Squasher");
        if (fracs.stream().allMatch(f -> f == 1.0)) badges.add("🧪 Test Champion");
        if (ivAnswered == d.interview().size() && ivAvg >= 0.75) badges.add("🎤 Interview Ace");
        if (ivAnswered == d.interview().size() && answered == mcqKeys.size()
                && fracs.stream().allMatch(f -> f > 0) && timeUsed > 0 && timeUsed <= 600) badges.add("⚡ Speed Runner");

        return new ScoreResult(mcq, code, interview, total, rank, badges);
    }

    public static int tier(int total) {
        return total >= 80 ? 3 : total >= 60 ? 2 : total >= 40 ? 1 : 0;
    }
}
