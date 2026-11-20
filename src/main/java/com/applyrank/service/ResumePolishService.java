package com.applyrank.service;

import com.applyrank.service.CodeReviewService.Finding;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resume Polish - the "rules" part.
 * Step 1: I hide private details (email, phone number, links) so they are never sent anywhere.
 * Step 2: I check the cleaned text for the things recruiters notice first
 *         (missing sections, weak words, no numbers, very long or very short).
 * The user can see exactly what the AI would get, because I return the cleaned text too.
 * Names and home addresses cannot be found reliably by rules, so the page tells the user to remove them.
 */
@Service
public class ResumePolishService {

    /** score = 0..100 (higher is better), redactions = how many items I hid, cleaned = the text without private details */
    public record Result(int score, int lines, int words, List<Finding> findings, Map<String, Integer> redactions, String cleaned) {}

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern LINK = Pattern.compile("(https?://\\S+|www\\.\\S+|(linkedin|github|gitlab|leetcode|behance)\\.com/\\S*)", Pattern.CASE_INSENSITIVE);
    // something that may be a phone number. I count its digits afterwards, so that year ranges like "2021-2025" are not hidden by mistake.
    private static final Pattern PHONE = Pattern.compile("(?<![\\w])\\+?\\d[\\d\\s().-]{6,17}\\d(?![\\w])");

    private static final Set<String> STRONG_VERBS = Set.of("built", "developed", "designed", "implemented", "created", "led", "reduced",
            "improved", "deployed", "optimized", "optimised", "automated", "integrated", "launched", "migrated", "tested", "fixed",
            "analyzed", "analysed", "wrote", "managed", "organized", "organised", "increased", "delivered", "trained", "engineered", "secured");

    private static final List<String> WEAK_WORDS = List.of("responsible for", "worked on", "helped with", "helped in", "duties included",
            "hard working", "hardworking", "team player", "go-getter", "result oriented", "result-oriented", "quick learner", "passionate about");

    public Result polish(String text) {
        // ---- step 1: hide private details ----
        Map<String, Integer> hidden = new LinkedHashMap<>();
        String cleaned = replaceAll(text, EMAIL, "[email]", hidden, "emails");
        cleaned = replaceAll(cleaned, LINK, "[link]", hidden, "links");
        cleaned = hidePhones(cleaned, hidden);

        // ---- step 2: look for problems in the cleaned text ----
        List<Finding> found = new ArrayList<>();
        String lower = cleaned.toLowerCase(Locale.ROOT);
        String[] lines = cleaned.split("\\R", -1);
        int words = cleaned.isBlank() ? 0 : cleaned.trim().split("\\s+").length;

        if (words < 80)
            found.add(new Finding("HIGH", 0, "Very short resume", "Add your projects, skills and education with a few lines each. Right now there is too little for a recruiter to judge."));
        else if (words > 900)
            found.add(new Finding("MEDIUM", 0, "Too long", "A fresher resume should fit on one page (about 400-600 words). Keep only your best projects."));

        if (!has(lower, "skills", "technologies", "tech stack"))
            found.add(new Finding("HIGH", 0, "No skills section", "Add a short 'Skills' section. Recruiters and automatic filters look for it first."));
        if (!has(lower, "project"))
            found.add(new Finding("HIGH", 0, "No projects section", "For a fresher, projects are the proof of skill. Add 2-3 projects with what you built and which tools you used."));
        if (!has(lower, "education", "b.tech", "btech", "b.e", "bca", "mca", "b.sc", "bsc", "degree", "diploma"))
            found.add(new Finding("MEDIUM", 0, "No education section", "Add your degree, college and year."));
        if (!has(lower, "experience", "intern"))
            found.add(new Finding("LOW", 0, "No experience or internship", "If you have none, that is fine. Add 'Experience' with any internship, freelance work or open-source help you did."));

        if (hidden.getOrDefault("emails", 0) == 0)
            found.add(new Finding("MEDIUM", 0, "No email address found", "Add a professional email at the top (name@gmail.com is fine). Recruiters must be able to reach you."));
        if (hidden.getOrDefault("links", 0) == 0)
            found.add(new Finding("LOW", 0, "No GitHub / LinkedIn / project link", "Add your GitHub or a live project link. A link lets a recruiter see your real work."));

        // lines that look like bullet points
        List<String> bullets = new ArrayList<>();
        int digitLines = 0;
        for (String l : lines) {
            String t = l.strip();
            if (t.startsWith("-") || t.startsWith("*") || t.startsWith("•")) {
                bullets.add(t.replaceFirst("^[-*•]\\s*", ""));
            }
            if (t.matches(".*\\d.*") && !t.contains("[phone]")) digitLines++;
        }
        if (bullets.size() >= 3) {
            long strong = bullets.stream().filter(b -> {
                String first = b.split("\\s+", 2)[0].toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
                return STRONG_VERBS.contains(first);
            }).count();
            if (strong * 100 / bullets.size() < 30)
                found.add(new Finding("MEDIUM", 0, "Bullets do not start with strong action verbs",
                        "Start each bullet with a verb like Built, Designed, Reduced, Deployed. Example: 'Built a login API with JWT' is stronger than 'Responsible for login'."));
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].strip().length() > 220) {
                    found.add(new Finding("LOW", i + 1, "Very long line", "Split it into two short bullets. Recruiters read a resume in about 10 seconds."));
                    break;
                }
            }
        } else if (words >= 80) {
            found.add(new Finding("LOW", 0, "Few or no bullet points", "Write your projects and experience as short bullets starting with '-' so they are easy to scan."));
        }
        if (digitLines < 3 && words >= 80)
            found.add(new Finding("MEDIUM", 0, "Almost no numbers", "Add numbers where you can: 'handled 500 users', 'cut load time by 30%', '3 REST APIs', 'score 8.4 CGPA'."));

        for (String w : WEAK_WORDS) {
            int at = lower.indexOf(w);
            if (at >= 0) {
                found.add(new Finding("MEDIUM", lineOf(cleaned, at), "Weak phrase: \"" + w + "\"",
                        "Say what you actually did and what came out of it, instead of a general phrase. Show it with a project, not a label."));
                break;       // one finding for this is enough, it should not take over the whole list
            }
        }
        if (Pattern.compile("(?m)^\\s*(i am|i have|i would|my name)\\b", Pattern.CASE_INSENSITIVE).matcher(cleaned).find())
            found.add(new Finding("LOW", 0, "Sentences start with \"I\"", "A resume is written without 'I'. Write 'Built a chat app' instead of 'I built a chat app'."));
        if (has(lower, "seeking a challenging", "challenging position", "career objective"))
            found.add(new Finding("MEDIUM", 0, "Generic objective line", "Replace the objective with a 2-line summary: your track, your best project and your key tools."));
        if (has(lower, "date of birth", "dob", "marital status", "father's name", "nationality", "religion"))
            found.add(new Finding("LOW", 0, "Personal details you do not need", "Remove date of birth, marital status, religion and similar details. They are not needed and can be misused."));

        found.sort((a, b) -> {
            int s = Integer.compare(rank(a.severity()), rank(b.severity()));
            return s != 0 ? s : Integer.compare(a.line(), b.line());
        });
        int score = 100;
        for (Finding f : found) score -= switch (f.severity()) { case "HIGH" -> 15; case "MEDIUM" -> 7; default -> 3; };
        return new Result(Math.max(0, score), lines.length, words, found, hidden, cleaned);
    }

    private static String replaceAll(String text, Pattern p, String with, Map<String, Integer> counts, String key) {
        Matcher m = p.matcher(text);
        int n = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find()) { n++; m.appendReplacement(sb, Matcher.quoteReplacement(with)); }
        m.appendTail(sb);
        counts.put(key, n);
        return sb.toString();
    }

    /** Hides a number only if it has at least 10 digits (or 8 digits with a + in front), like a real phone number. */
    private static String hidePhones(String text, Map<String, Integer> counts) {
        Matcher m = PHONE.matcher(text);
        int n = 0;
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String found = m.group();
            int digits = found.replaceAll("\\D", "").length();
            boolean phone = digits >= 10 || (found.startsWith("+") && digits >= 8);
            if (phone) n++;
            m.appendReplacement(sb, Matcher.quoteReplacement(phone ? "[phone]" : found));
        }
        m.appendTail(sb);
        counts.put("phones", n);
        return sb.toString();
    }

    private static boolean has(String lower, String... words) {
        for (String w : words) if (lower.contains(w)) return true;
        return false;
    }

    private static int lineOf(String text, int index) {
        int line = 1;
        for (int i = 0; i < index; i++) if (text.charAt(i) == '\n') line++;
        return line;
    }

    private static int rank(String severity) {
        return switch (severity) { case "HIGH" -> 0; case "MEDIUM" -> 1; default -> 2; };
    }
}
