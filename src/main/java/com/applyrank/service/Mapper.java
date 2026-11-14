package com.applyrank.service;

import com.applyrank.dto.Dtos.*;
import com.applyrank.model.*;
import com.applyrank.repo.TestResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Entity -> DTO conversion in one place. */
@Component
public class Mapper {
    public static final String[] STAGES = {"Applied", "Resume check", "Rank check", "Skill & interview", "Final decision"};

    private final ObjectMapper json;
    private final TestResultRepository results;

    public Mapper(ObjectMapper json, TestResultRepository results) {
        this.json = json;
        this.results = results;
    }

    public UserDto user(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getName(), u.getRole().name());
    }

    public static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public static String join(List<String> list) {
        return list == null ? "" : String.join(",", list.stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList());
    }

    public JobDto job(Job j) {
        return new JobDto(j.getId(), j.getTitle(), j.getCompany(), j.getLocation(), j.getTrack(), j.getCtc(),
                j.getDescription(), split(j.getRequirements()), j.getMinRank(), j.getCreatedAt().toString());
    }

    public ApplicationDto application(JobApplication a) {
        int stage = Math.max(0, Math.min(4, a.getStage()));
        return new ApplicationDto(a.getId(), job(a.getJob()), stage, STAGES[stage], a.getOutcome().name(),
                a.getAppliedAt().toString(), a.getRankAtApply(), a.getInterviewOn(), a.getNote());
    }

    public TestResultDto test(TestResult t) {
        return new TestResultDto(t.getId(), t.getDomain(), t.getTotal(), t.getMcq(), t.getCode(), t.getInterview(),
                t.getRankTitle(), t.getXp(), t.getTimeUsed(), split(t.getBadges()), t.getCreatedAt().toString(), true);
    }

    public NotificationDto notification(Notification n) {
        CtaDto cta = n.getCtaLabel() == null ? null : new CtaDto(n.getCtaLabel(), n.getCtaKind(), n.getCtaArg());
        return new NotificationDto(n.getId(), n.getCategory(), n.getTitle(), n.getDescription(), n.isSeen(),
                n.getCreatedAt().toString(), cta);
    }

    public ApplicantDto applicant(JobApplication a, TestResult latest) {
        User u = a.getUser();
        boolean reveal = a.getOutcome() == Outcome.SHORTLISTED || a.getOutcome() == Outcome.HIRED;   // privacy by design
        return new ApplicantDto(a.getId(), u.getId(), u.getName(), reveal ? u.getEmail() : maskEmail(u.getEmail()), null, null,
                latest == null ? null : latest.getTotal(), latest == null ? null : latest.getRankTitle(),
                a.getStage(), a.getOutcome().name(), a.getAppliedAt().toString());
    }

    /** a***@gmail.com - recruiters only see the full e-mail after they shortlist the candidate. */
    public static String maskEmail(String e) {
        if (e == null || !e.contains("@")) return "***";
        int at = e.indexOf('@');
        return e.substring(0, Math.min(1, at)) + "***" + e.substring(at);
    }

    public ProfileDto profile(User u, Profile p) {
        List<Integer> self = new ArrayList<>();
        for (String s : split(p.getSelfRatings())) {
            try { self.add(Integer.parseInt(s)); } catch (NumberFormatException e) { self.add(0); }
        }
        return new ProfileDto(u.getName(), p.getHeadline(), u.getEmail(), p.getPhone(), p.getLocation(), p.getPhoto(),
                p.getAbout(), p.getStatus(), p.getExpYears(), p.getCompany(), p.getJobRole(), p.getExpDesc(),
                p.getDegree(), p.getUniversity(), p.getGradYear(), p.getCgpa(), p.getSkills(), p.getGithub(),
                p.getLinkedin(), self, parse(p.getProjectsJson()), parse(p.getCertsJson()));
    }

    private JsonNode parse(String s) {
        try { return json.readTree(s == null || s.isBlank() ? "[]" : s); }
        catch (Exception e) { return json.createArrayNode(); }
    }
}
