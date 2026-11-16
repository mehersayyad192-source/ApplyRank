package com.applyrank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Tests the AI Lab API. No API key is set here, so these tests also prove the "no AI" mode works. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:ailabdb;DB_CLOSE_DELAY=-1", "app.upload-dir=./target/test-uploads", "app.ai.api-key="})
class AiLabApiTest {
    @Autowired MockMvc mvc;

    private String login(String email) throws Exception {
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"secret12\",\"name\":\"Lab\"}")).andReturn().getResponse().getContentAsString();
        return "Bearer " + res.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void needsLogin() throws Exception {
        mvc.perform(get("/api/ailab/status")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ailab/code-review").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void statusShowsAiIsOffWithoutKey() throws Exception {
        mvc.perform(get("/api/ailab/status").header("Authorization", login("lab1@x.dev")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.aiEnabled").value(false));
    }

    @Test
    void emptyAndHugeCodeAreRejected() throws Exception {
        String t = login("lab2@x.dev");
        mvc.perform(post("/api/ailab/code-review").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"   \"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/ailab/code-review").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + "a".repeat(13000) + "\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void rulesReviewStreamsAndIsSavedInHistory() throws Exception {
        String t = login("lab3@x.dev");
        String out = mvc.perform(post("/api/ailab/code-review").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"var a = 1;\\nif (a == 2) { eval(a); }\",\"useAi\":true,\"consent\":true}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(out.contains("event: rules"), out);
        assertTrue(out.contains("event: done"), out);
        assertTrue(out.contains("eval"), out);

        mvc.perform(get("/api/ailab/history").header("Authorization", t))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].tool").value("CODE_REVIEW"))
                .andExpect(jsonPath("$[0].usedAi").value(false));   // no key, so the AI was not used
    }

    @Test
    void historyIsPrivatePerUser() throws Exception {
        String a = login("lab4@x.dev");
        mvc.perform(post("/api/ailab/code-review").header("Authorization", a).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"const a = 1;\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/ailab/history").header("Authorization", login("lab5@x.dev")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    // ---- Scam Detector and Page Auditor use the same streaming engine ----
    @Test
    void scamCheckStreamsRulesAndNeedsLogin() throws Exception {
        mvc.perform(post("/api/ailab/scam-check").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        String t = login("lab6@x.dev");
        String out = mvc.perform(post("/api/ailab/scam-check").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Pay registration fee today and join on WhatsApp without interview. Earn 5000 per day!\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(out.contains("event: rules") && out.contains("event: done"), out);
        assertTrue(out.contains("Asks you to pay money"), out);
        mvc.perform(post("/api/ailab/scam-check").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"  \"}")).andExpect(status().isBadRequest());
    }

    @Test
    void pageAuditStreamsRulesAndHistoryShowsBothTools() throws Exception {
        String t = login("lab7@x.dev");
        String out = mvc.perform(post("/api/ailab/page-audit").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"<html><body><img src='a.png'></body></html>\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(out.contains("event: rules") && out.contains("Missing <title>"), out);
        mvc.perform(post("/api/ailab/scam-check").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Pay the registration fee\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/ailab/history").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tool").value("SCAM_CHECK"))
                .andExpect(jsonPath("$[1].tool").value("PAGE_AUDIT"));
    }

    @Test
    void pageAuditAcceptsLongerTextThanCodeButNotUnlimited() throws Exception {
        String t = login("lab8@x.dev");
        String longHtml = "<p>" + "a".repeat(20000) + "</p>";            // longer than the 12000 code limit, but under the 40000 page limit
        mvc.perform(post("/api/ailab/page-audit").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"" + longHtml + "\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/ailab/page-audit").header("Authorization", t).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"" + "a".repeat(41000) + "\"}")).andExpect(status().isBadRequest());
    }
}
