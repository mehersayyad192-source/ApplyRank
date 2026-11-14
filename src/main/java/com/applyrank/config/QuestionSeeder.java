package com.applyrank.config;

import com.applyrank.model.Question;
import com.applyrank.repo.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/** Loads src/main/resources/questions-bank.json into the questions table on first start. */
@Component
public class QuestionSeeder implements CommandLineRunner {
    record Item(String domain, String type, String q, String code, List<String> options, int answer, String why) {}

    private final QuestionRepository repo;
    private final ObjectMapper json;

    public QuestionSeeder(QuestionRepository repo, ObjectMapper json) {
        this.repo = repo;
        this.json = json;
    }

    @Override
    public void run(String... args) throws Exception {
        if (repo.count() > 0) return;
        try (InputStream in = new ClassPathResource("questions-bank.json").getInputStream()) {
            List<Item> items = json.readValue(in, new TypeReference<List<Item>>() {});
            for (Item i : items) {
                Question q = new Question();
                q.setDomain(i.domain());
                q.setType(i.type());
                q.setPrompt(i.q());
                q.setSnippet(i.code());
                q.setOptionsJson(json.writeValueAsString(i.options()));
                q.setCorrectIndex(i.answer());
                q.setExplanation(i.why());
                repo.save(q);
            }
        }
    }
}
