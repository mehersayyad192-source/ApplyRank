package com.applyrank.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Request / response shapes of the REST API (Java records = tiny immutable classes). */
public final class Dtos {
    private Dtos() {}

    // ---------- auth ----------
    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters.")
                                  @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password needs at least one letter and one number.") String password,
                                  @NotBlank @Size(max = 80) String name,
                                  String role) {}
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record UserDto(Long id, String email, String name, String role, boolean emailVerified) {}
    public record ForgotRequest(@NotBlank @Email String email) {}
    public record ResetRequest(@NotBlank String token,
                               @NotBlank @Size(min = 8, max = 72, message = "Password must be 8-72 characters.")
                               @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password needs at least one letter and one number.") String password) {}
    public record VerifyRequest(@NotBlank String token) {}
    public record AuthResponse(String token, UserDto user) {}

    // ---------- profile (same field names the frontend profile page already uses) ----------
    public record ProfileDto(String name, String headline, String email, String phone, String location,
                             String photo, String about, String status, Double expYears, String company,
                             String role, String expDesc, String degree, String university, String gradYear,
                             String cgpa, String skills, String github, String linkedin,
                             List<Integer> self, JsonNode projects, JsonNode certs) {}

    // ---------- jobs & applications ----------
    public record JobDto(Long id, String title, String company, String location, String track, String ctc,
                         String description, List<String> requirements, int minRank, String postedAt) {}
    public record JobRequest(@NotBlank String title, @NotBlank String company, String location, String track,
                             String ctc, String description, List<String> requirements, Integer minRank) {}
    public record ApplicationDto(Long id, JobDto job, int stage, String stageName, String outcome,
                                 String appliedAt, Integer rankAtApply, String interviewOn, String note) {}
    public record ApplicantDto(Long applicationId, Long userId, String name, String email, String headline,
                               String skills, Integer verifiedRank, String rankTitle, int stage, String outcome,
                               String appliedAt) {}
    public record StatusUpdateRequest(Integer stage, String outcome, String interviewOn, String note) {}

    // ---------- skill test ----------
    public record InterviewScoreRequest(@NotBlank String domain, String question, @NotBlank String answer) {}
    public record InterviewScoreResponse(double score, List<String> covered, List<String> missed, String feedback) {}
    public record TestSubmitRequest(@NotBlank String domain,
                                    @NotBlank String sessionId,        // MCQ answers are read from this server-side session
                                    @NotNull List<Double> code,        // fraction of tests passed per coding problem (0..1)
                                    List<String> interview,            // the candidate's typed answers, in order
                                    Integer timeUsed, Integer bestStreak, Integer xp) {}
    public record QuestionDto(Long id, String type, String q, String code, List<String> options) {}   // NO correct answer here
    public record TestSessionDto(String sessionId, String domain, List<QuestionDto> questions) {}
    public record AnswerRequest(@NotBlank String sessionId, @NotNull Long questionId, @NotNull Integer choice) {}
    public record AnswerResponse(boolean correct, int answer, String why) {}
    public record TestResultDto(Long id, String domain, int total, int mcq, int code, int interview, String rank,
                                int xp, int timeUsed, List<String> badges, String at, boolean verified) {}
    public record DomainDto(String key, String name) {}

    // ---------- notifications ----------
    public record CtaDto(String label, String kind, String arg) {}
    public record NotificationDto(Long id, String category, String title, String description, boolean read,
                                  String at, CtaDto cta) {}

    // ---------- resume ----------
    public record ResumeAnalysisDto(Long id, String filename, String track, int score, List<String> matched,
                                    List<String> missing, List<String> tips, String at, String text) {}
}
