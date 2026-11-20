package com.applyrank.config;

import com.applyrank.model.Question;
import com.applyrank.repo.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loads src/main/resources/questions-bank.json into the questions table. Each track is added only if it has no questions yet. */
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
        try (InputStream in = new ClassPathResource("questions-bank.json").getInputStream()) {
            List<Item> items = json.readValue(in, new TypeReference<List<Item>>() {});
            // I check every track on its own. A track that already has questions is left alone (nothing is added twice),
            // and a track with no questions yet gets all of its questions. This matters when I add a new track later:
            // the old version only looked at the whole table, so an existing database never received the new track.
            Set<String> alreadyThere = new HashSet<>();
            for (Item i : items) {
                if (!alreadyThere.contains(i.domain()) && !repo.idsByDomain(i.domain()).isEmpty()) {
                    alreadyThere.add(i.domain());
                }
            }
            for (Item i : items) {
                if (alreadyThere.contains(i.domain())) continue;
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
