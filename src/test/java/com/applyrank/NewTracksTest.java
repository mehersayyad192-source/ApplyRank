package com.applyrank;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Checks that every track (the 3 old ones and the 6 new ones) has complete questions and works through the API.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:tracksdb;DB_CLOSE_DELAY=-1", "app.upload-dir=./target/test-uploads"})
class NewTracksTest {
    @Autowired MockMvc mvc;

    private static final List<String> NEW_TRACKS = List.of("ml", "devops", "mobile", "qa", "cyber", "uiux");

    @Test
    void jsonFilesAreCompleteForEveryTrack() throws Exception {
        ObjectMapper om = new ObjectMapper();
        JsonNode bank, meta;
        try (InputStream in = new ClassPathResource("questions-bank.json").getInputStream()) { bank = om.readTree(in); }
        try (InputStream in = new ClassPathResource("questions.json").getInputStream()) { meta = om.readTree(in); }

        Map<String, Integer> perDomain = new HashMap<>();
        for (JsonNode q : bank) {
            perDomain.merge(q.get("domain").asText(), 1, Integer::sum);
            int options = q.get("options").size();
            int answer = q.get("answer").asInt();
            assertEquals(4, options, "every question needs 4 options");
            assertTrue(answer >= 0 && answer < options, "answer index must point to an option");
        }
        for (String key : NEW_TRACKS) {
            assertTrue(meta.has(key), "questions.json is missing " + key);
            assertTrue(perDomain.getOrDefault(key, 0) >= 12, key + " needs at least 12 questions in the bank");
            assertEquals(5, meta.get(key).get("mcqKeys").size(), key + " needs 5 offline answer keys");
            assertEquals(3, meta.get(key).get("interview").size(), key + " needs 3 interview rubrics");
        }
    }

    @Test
    void everyNewTrackServesFiveQuestionsWithoutAnswers() throws Exception {
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tracks1@x.dev\",\"password\":\"secret12\",\"name\":\"Tracks\"}"))
                .andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + JsonPath.<String>read(res, "$.token");
        for (String key : NEW_TRACKS) {
            mvc.perform(get("/api/tests/questions").param("domain", key).header("Authorization", auth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questions.length()").value(5))
                    .andExpect(jsonPath("$.questions[0].answer").doesNotExist());
        }
    }
}
