package com.applyrank;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:jobsearchdb;DB_CLOSE_DELAY=-1", "app.upload-dir=./target/test-uploads"})
class JobSearchTest {
    @Autowired MockMvc mvc;

    @Test
    void searchWordsMustAllMatch() throws Exception {
        mvc.perform(get("/api/jobs?q=java pune")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].location").value(containsStringIgnoringCase("pune")));
        mvc.perform(get("/api/jobs?q=zzzznotfound")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void severalCitiesAtOnceAndPagingHeader() throws Exception {
        mvc.perform(get("/api/jobs?location=Pune,Hyderabad&page=0&size=10")).andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void rankBarFilterAndTrackFilterWork() throws Exception {
        mvc.perform(get("/api/jobs?maxRank=0")).andExpect(status().isOk());
        mvc.perform(get("/api/jobs?track=backend&sort=rank_high")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].track").value("backend"));
    }

    @Test
    void typedWildcardsAreNotTreatedAsWildcards() throws Exception {
        mvc.perform(get("/api/jobs?q=___")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void locationListHasCities() throws Exception {
        mvc.perform(get("/api/jobs/locations")).andExpect(status().isOk()).andExpect(jsonPath("$").value(hasItem("Pune")));
    }

    @Test
    void savedJobsNeedLoginAndRoundTrip() throws Exception {
        mvc.perform(get("/api/saved-jobs")).andExpect(status().isUnauthorized());
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"demo@applyrank.dev\",\"password\":\"Demo@1234\"}")).andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + res.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mvc.perform(put("/api/saved-jobs/1").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(put("/api/saved-jobs/1").header("Authorization", auth)).andExpect(status().isOk());   // saving twice is fine
        mvc.perform(get("/api/saved-jobs").header("Authorization", auth)).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(delete("/api/saved-jobs/1").header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get("/api/saved-jobs").header("Authorization", auth)).andExpect(jsonPath("$.length()").value(0));
    }
}
