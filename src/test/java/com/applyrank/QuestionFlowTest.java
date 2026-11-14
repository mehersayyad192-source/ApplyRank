package com.applyrank;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:flowdb;DB_CLOSE_DELAY=-1", "app.upload-dir=./target/test-uploads"})
class QuestionFlowTest {
    @Autowired MockMvc mvc;

    private String register(String email) throws Exception {
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"secret12\",\"name\":\"Flow\"}"))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.<String>read(res, "$.token");
    }

    @Test
    void serverServesRandomQuestionsWithoutAnswersAndGradesFromItsOwnSession() throws Exception {
        String auth = register("flow1@x.dev");
        String body = mvc.perform(get("/api/tests/questions").param("domain", "backend").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(5))
                .andExpect(jsonPath("$.questions[0].answer").doesNotExist())          // the answer key never leaves the server
                .andExpect(jsonPath("$.questions[0].correctIndex").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String sid = JsonPath.read(body, "$.sessionId");
        List<Integer> ids = JsonPath.read(body, "$.questions[*].id");

        int correct = 0;
        for (Integer id : ids) {
            String ans = mvc.perform(post("/api/tests/answer").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"sessionId\":\"" + sid + "\",\"questionId\":" + id + ",\"choice\":0}"))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            if (JsonPath.<Boolean>read(ans, "$.correct")) correct++;
        }
        // an answer is locked: answering the same question again is rejected
        mvc.perform(post("/api/tests/answer").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"" + sid + "\",\"questionId\":" + ids.get(0) + ",\"choice\":1}"))
                .andExpect(status().isConflict());

        String submit = "{\"domain\":\"backend\",\"sessionId\":\"" + sid + "\",\"code\":[0,0],\"interview\":[],\"timeUsed\":300,\"bestStreak\":0,\"xp\":0}";
        String res = mvc.perform(post("/api/tests/submit").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(submit))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertEquals(correct * 8, JsonPath.<Integer>read(res, "$.mcq"));            // 8 points per correct MCQ, computed by the server

        // the same session cannot be submitted twice
        mvc.perform(post("/api/tests/submit").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content(submit))
                .andExpect(status().isConflict());
    }

    @Test
    void anotherUserCannotUseMySession() throws Exception {
        String a = register("flow2@x.dev"), b = register("flow3@x.dev");
        String body = mvc.perform(get("/api/tests/questions").param("domain", "data").header("Authorization", a))
                .andReturn().getResponse().getContentAsString();
        String sid = JsonPath.read(body, "$.sessionId");
        Integer id = JsonPath.read(body, "$.questions[0].id");
        mvc.perform(post("/api/tests/answer").header("Authorization", b).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"" + sid + "\",\"questionId\":" + id + ",\"choice\":0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownTrackIsRejected() throws Exception {
        mvc.perform(get("/api/tests/questions").param("domain", "cooking").header("Authorization", register("flow4@x.dev")))
                .andExpect(status().isBadRequest());
    }
}
