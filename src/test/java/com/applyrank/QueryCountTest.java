package com.applyrank;

import com.applyrank.model.*;
import com.applyrank.repo.*;
import com.applyrank.security.JwtService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proof that the recruiter's applicant list has NO N+1 problem: with 15 applicants the number of SQL statements
 * must stay small and constant (before the fix it was ~2 extra queries per applicant).
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:qcdb;DB_CLOSE_DELAY=-1",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "app.upload-dir=./target/test-uploads"})
class QueryCountTest {
    @Autowired MockMvc mvc;
    @Autowired EntityManagerFactory emf;
    @Autowired UserRepository users;
    @Autowired JobRepository jobs;
    @Autowired JobApplicationRepository apps;
    @Autowired TestResultRepository results;
    @Autowired JwtService jwt;

    private User user(String email, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setName(email);
        u.setRole(role);
        u.setPasswordHash("x");
        return users.save(u);
    }

    @Test
    void applicantListUsesAConstantNumberOfQueries() throws Exception {
        User rec = user("qc-rec@x.dev", Role.RECRUITER);
        Job job = new Job();
        job.setTitle("Java Dev");
        job.setCompany("Acme");
        job.setPostedBy(rec);
        job = jobs.save(job);
        for (int i = 0; i < 15; i++) {
            User c = user("qc-c" + i + "@x.dev", Role.CANDIDATE);
            JobApplication a = new JobApplication();
            a.setUser(c);
            a.setJob(job);
            apps.save(a);
            TestResult t = new TestResult();
            t.setUser(c);
            t.setDomain("backend");
            t.setTotal(40 + i);
            t.setRankTitle("Tester");
            results.save(t);
        }

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();
        mvc.perform(get("/api/recruiter/jobs/" + job.getId() + "/applicants")
                        .header("Authorization", "Bearer " + jwt.generate(rec.getEmail(), "RECRUITER")))
                .andExpect(status().isOk());
        long statements = stats.getPrepareStatementCount();
        // 12 (was 10): the login filter now does one extra lookup to check the token version. An N+1 bug would still give 30+.
        assertTrue(statements <= 12, "Expected a handful of SQL statements, but got " + statements + " (N+1 problem?)");
    }
}
