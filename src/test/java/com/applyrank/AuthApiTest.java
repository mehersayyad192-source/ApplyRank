package com.applyrank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "app.upload-dir=./target/test-uploads"})
class AuthApiTest {
    @Autowired MockMvc mvc;

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void jobsArePublicButApplicationsNeedLogin() throws Exception {
        mvc.perform(get("/api/jobs")).andExpect(status().isOk());
        mvc.perform(get("/api/applications")).andExpect(status().isUnauthorized());
    }

    @Test
    void registerThenLoginWorksAndDuplicateIsRejected() throws Exception {
        String body = "{\"email\":\"t1@x.dev\",\"password\":\"secret12\",\"name\":\"Tess\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"t1@x.dev\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void candidateCannotUseRecruiterEndpoints() throws Exception {
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"t2@x.dev\",\"password\":\"secret12\",\"name\":\"Cam\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = res.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mvc.perform(get("/api/recruiter/jobs").header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void weakPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"weak@x.dev\",\"password\":\"abcdefgh\",\"name\":\"Weak\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fiveWrongLoginsLockTheAccountTemporarily() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"lock@x.dev\",\"password\":\"secret12\",\"name\":\"Lock\"}")).andExpect(status().isCreated());
        String bad = "{\"email\":\"lock@x.dev\",\"password\":\"nope1234\"}";
        for (int i = 0; i < 5; i++)
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(bad)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lock@x.dev\",\"password\":\"secret12\"}"))     // even the RIGHT password is blocked now
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void userCanExportAndThenDeleteTheirData() throws Exception {
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gone@x.dev\",\"password\":\"secret12\",\"name\":\"Gone\"}"))
                .andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + res.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mvc.perform(get("/api/privacy/export").header("Authorization", auth))
                .andExpect(status().isOk()).andExpect(jsonPath("$.account.email").value("gone@x.dev"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/privacy/account")
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"wrong\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/privacy/account")
                        .header("Authorization", auth).contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"secret12\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"gone@x.dev\",\"password\":\"secret12\"}"))
                .andExpect(status().isUnauthorized());
    }
}
