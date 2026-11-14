package com.applyrank.service;

import com.applyrank.dto.Dtos.*;
import com.applyrank.model.Question;
import com.applyrank.model.TestSession;
import com.applyrank.model.User;
import com.applyrank.repo.QuestionRepository;
import com.applyrank.repo.TestSessionRepository;
import com.applyrank.web.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Serves a random set of 5 questions per attempt, WITHOUT the answers, and checks answers on the server.
 * The browser can't read the answer key, and the stored session answers (not the browser's claims) are graded.
 */
@Service
public class QuestionService {
    public static final int PER_TEST = 5;
    private static final Duration MAX_AGE = Duration.ofHours(2);

    public record Graded(List<Integer> keys, List<Integer> answers) {}

    private final QuestionRepository questions;
    private final TestSessionRepository sessions;
    private final ObjectMapper json;
    private final SecureRandom random = new SecureRandom();

    public QuestionService(QuestionRepository questions, TestSessionRepository sessions, ObjectMapper json) {
        this.questions = questions;
        this.sessions = sessions;
        this.json = json;
    }

    public List<String> options(Question q) {
        try {
            return json.readValue(q.getOptionsJson(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Bad options JSON for question " + q.getId(), e);
        }
    }

    @Transactional
    public TestSessionDto start(User user, String domain) {
        String key = domain == null ? "" : domain.toLowerCase();
        List<Long> ids = new ArrayList<>(questions.idsByDomain(key));   // light query: ids only
        if (ids.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown track.");
        Collections.shuffle(ids, random);
        List<Long> pick = new ArrayList<>(ids.subList(0, Math.min(PER_TEST, ids.size())));

        Map<Long, Question> byId = questions.findAllById(pick).stream().collect(Collectors.toMap(Question::getId, q -> q));
        TestSession s = new TestSession();
        s.setId(UUID.randomUUID().toString());
        s.setUser(user);
        s.setDomain(key);
        s.setQuestionIds(pick.stream().map(String::valueOf).collect(Collectors.joining(",")));
        s.setAnswers(pick.stream().map(i -> "-").collect(Collectors.joining(",")));
        sessions.save(s);

        List<QuestionDto> dtos = pick.stream().map(byId::get)
                .map(q -> new QuestionDto(q.getId(), q.getType(), q.getPrompt(), q.getSnippet(), options(q))).toList();
        return new TestSessionDto(s.getId(), key, dtos);
    }

    /** Locks in ONE answer (it cannot be changed later) and returns the correct option + explanation. */
    @Transactional
    public AnswerResponse answer(User user, AnswerRequest r) {
        TestSession s = open(user, r.sessionId());
        List<Long> ids = longs(s.getQuestionIds());
        int pos = ids.indexOf(r.questionId());
        if (pos < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "That question is not part of this test.");
        String[] ans = s.getAnswers().split(",", -1);
        if (!"-".equals(ans[pos])) throw new ApiException(HttpStatus.CONFLICT, "You already answered this question.");

        Question q = questions.findById(r.questionId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found."));
        int n = options(q).size();
        if (r.choice() < 0 || r.choice() >= n) throw new ApiException(HttpStatus.BAD_REQUEST, "Choice must be 0-" + (n - 1) + ".");
        ans[pos] = String.valueOf(r.choice());
        s.setAnswers(String.join(",", ans));
        sessions.save(s);
        return new AnswerResponse(r.choice() == q.getCorrectIndex(), q.getCorrectIndex(), q.getExplanation());
    }

    /** Finishes the session (only once) and returns the answer key + the stored answers, in served order. */
    @Transactional
    public Graded close(User user, String sessionId, String domain) {
        TestSession s = open(user, sessionId);
        if (domain == null || !s.getDomain().equalsIgnoreCase(domain)) throw new ApiException(HttpStatus.BAD_REQUEST, "Track does not match this test.");
        s.setSubmitted(true);                                   // @Version: a parallel double submit fails with 409
        sessions.saveAndFlush(s);

        List<Long> ids = longs(s.getQuestionIds());
        Map<Long, Question> byId = questions.findAllById(ids).stream().collect(Collectors.toMap(Question::getId, q -> q));
        List<Integer> keys = ids.stream().map(i -> byId.get(i).getCorrectIndex()).toList();
        List<Integer> answers = new ArrayList<>();
        for (String a : s.getAnswers().split(",", -1)) answers.add("-".equals(a) ? null : Integer.valueOf(a));
        return new Graded(keys, answers);
    }

    private TestSession open(User user, String id) {
        TestSession s = sessions.findByIdAndUser(id, user).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test session not found."));
        if (s.isSubmitted()) throw new ApiException(HttpStatus.CONFLICT, "This test was already submitted.");
        if (s.getCreatedAt().plus(MAX_AGE).isBefore(Instant.now())) throw new ApiException(HttpStatus.BAD_REQUEST, "This test session expired. Please start again.");
        return s;
    }

    private static List<Long> longs(String csv) {
        return Arrays.stream(csv.split(",")).map(Long::valueOf).toList();
    }
}
