package com.applyrank.config;

import com.applyrank.model.*;
import com.applyrank.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** First start only: creates demo logins and a few jobs (all companies are fictional). */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final JobRepository jobs;
    private final PasswordEncoder encoder;
    private final String candidatePassword;
    private final String recruiterPassword;

    /**
     * The demo passwords come from settings. On your own computer they have easy defaults (see application.properties).
     * On the LIVE server (prod profile) there is no default: if you do not set DEMO_CANDIDATE_PASSWORD / DEMO_RECRUITER_PASSWORD,
     * the account gets a random password nobody knows. Otherwise anyone reading GitHub could log in as the recruiter.
     */
    public DataSeeder(UserRepository users, ProfileRepository profiles, JobRepository jobs, PasswordEncoder encoder,
                      @Value("${app.demo.candidate-password:}") String candidatePassword,
                      @Value("${app.demo.recruiter-password:}") String recruiterPassword) {
        this.users = users; this.profiles = profiles; this.jobs = jobs; this.encoder = encoder;
        this.candidatePassword = candidatePassword; this.recruiterPassword = recruiterPassword;
    }

    private static String orRandom(String pw) {
        return (pw == null || pw.isBlank()) ? UUID.randomUUID() + "-" + UUID.randomUUID() : pw;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        User rec = user("recruiter@applyrank.dev", orRandom(recruiterPassword), "Demo Recruiter", Role.RECRUITER);
        user("demo@applyrank.dev", orRandom(candidatePassword), "Demo Candidate", Role.CANDIDATE);

        job(rec, "React Frontend Developer", "Nimbus Labs", "Remote", "frontend", "6-9 LPA", 40, "React,JavaScript,CSS,Git",
                "Build fast, accessible interfaces for a SaaS dashboard.");
        job(rec, "UI Engineer (Fresher)", "Pixelforge", "Bengaluru", "frontend", "4-6 LPA", 20, "HTML,CSS,JavaScript",
                "Turn designs into pixel-perfect responsive pages.");
        job(rec, "Java Backend Developer", "Brightloop", "Hyderabad", "backend", "7-10 LPA", 60, "Java,Spring Boot,SQL,REST",
                "Design REST APIs and own services end to end.");
        job(rec, "Junior Java Developer", "Nimbus Labs", "Pune", "backend", "5-7 LPA", 40, "Java,JPA,Git",
                "Join the platform team and ship features with mentoring.");
        job(rec, "Data Analyst", "Brightloop", "Remote", "data", "5-8 LPA", 40, "SQL,Excel,Python",
                "Turn product data into dashboards and decisions.");
        job(rec, "Full Stack Developer", "Pixelforge", "Chennai", "fullstack", "8-12 LPA", 60, "Java,React,Spring Boot,SQL",
                "Work across React and Spring Boot on a hiring product.");
    }

    private User user(String email, String pw, String name, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setRole(role);
        u.setPasswordHash(encoder.encode(pw));
        u.setEmailVerified(true);                       // demo accounts need no e-mail check
        users.save(u);
        Profile p = new Profile();
        p.setUser(u);
        profiles.save(p);
        return u;
    }

    private void job(User by, String title, String company, String loc, String track, String ctc, int minRank, String req, String desc) {
        Job j = new Job();
        j.setTitle(title); j.setCompany(company); j.setLocation(loc); j.setTrack(track); j.setCtc(ctc);
        j.setMinRank(minRank); j.setRequirements(req); j.setDescription(desc); j.setPostedBy(by);
        jobs.save(j);
    }
}
