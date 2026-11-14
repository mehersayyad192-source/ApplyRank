package com.applyrank.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * The SERVER-side answer key and interview rubrics (src/main/resources/questions.json).
 * The browser never receives the correct answers from here, so they can't be read from the page.
 */
@Component
public class QuestionBank {
    public record Rubric(String label, List<String> words) {}
    public record InterviewQ(String q, List<Rubric> rubric) {}
    public record Domain(String name, List<String> titles, List<Integer> mcqKeys, List<InterviewQ> interview) {}

    private final ObjectMapper mapper;
    private Map<String, Domain> domains = Map.of();

    public QuestionBank(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void init() {
        try (InputStream in = new ClassPathResource("questions.json").getInputStream()) {
            domains = mapper.readValue(in,
                    mapper.getTypeFactory().constructMapType(java.util.LinkedHashMap.class, String.class, Domain.class));
        } catch (IOException e) {
            throw new IllegalStateException("Could not load questions.json", e);
        }
    }

    public Map<String, Domain> all() { return domains; }

    public Domain get(String key) {
        return key == null ? null : domains.get(key.toLowerCase());
    }
}
