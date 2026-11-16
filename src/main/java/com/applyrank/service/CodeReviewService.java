package com.applyrank.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Code Review Coach - the "rules" part.
 * It reads the pasted code line by line and looks for common beginner mistakes (security, style, bugs).
 * This part works with NO internet and NO AI, so the tool is never empty.
 * The AI (AiClient) only adds friendly extra advice on top of these findings.
 */
@Service
public class CodeReviewService {

    /** One problem found in the code. line = 0 means "about the whole file". */
    public record Finding(String severity, int line, String title, String tip) {}

    /** The full result: score out of 100, the language I guessed, and all the findings. */
    public record Review(int score, String language, int lines, List<Finding> findings) {}

    /** A single rule: which language it is for, what text pattern to look for, and what to tell the user. */
    private record Rule(String lang, Pattern pattern, String severity, String title, String tip) {}

    private static Rule rule(String lang, String regex, String sev, String title, String tip) {
        return new Rule(lang, Pattern.compile(regex, Pattern.CASE_INSENSITIVE), sev, title, tip);
    }

    // "any" rules run for every language. HIGH = security risk, MEDIUM = likely bug or bad habit, LOW = style.
    private static final List<Rule> RULES = List.of(
            rule("any", "(password|passwd|secret|api[_-]?key|token)\\s*[:=]\\s*[\"'][^\"']{4,}[\"']", "HIGH",
                    "Secret written inside the code",
                    "Never type passwords or keys in code. Put them in an environment variable and read them from there."),
            rule("any", "(select|insert|update|delete)\\b[^;\\n]*[\"']\\s*\\+\\s*\\w+", "HIGH",
                    "SQL built by joining text (SQL injection risk)",
                    "Use a prepared statement with ? placeholders so user input can never change the query."),
            rule("any", "\\b(todo|fixme|hack)\\b", "LOW",
                    "TODO / FIXME left in the code",
                    "Finish it or remove it before you show this project to a recruiter."),

            rule("java", "System\\.out\\.print", "LOW",
                    "System.out.println used for output",
                    "Use a logger (SLF4J) so messages can be switched on or off and do not leak data."),
            rule("java", "catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}", "HIGH",
                    "Empty catch block",
                    "An empty catch hides errors. At least log it, or handle it properly."),
            rule("java", "catch\\s*\\(\\s*(Exception|Throwable)\\s+\\w+\\s*\\)", "MEDIUM",
                    "Catching the very general Exception",
                    "Catch the specific exception you expect (for example IOException) so real bugs are not hidden."),
            rule("java", "printStackTrace\\s*\\(", "MEDIUM",
                    "printStackTrace() used",
                    "Log the error with a logger instead. printStackTrace can show internal details."),
            rule("java", "(\\w+|\\))\\s*(==|!=)\\s*\"", "HIGH",
                    "Strings compared with == or !=",
                    "In Java use .equals() to compare text. == only checks if it is the same object."),
            rule("java", "\\bpublic\\s+(static\\s+)?(?!final|class|interface|enum|record|void|abstract)\\w[\\w<>\\[\\],]*\\s+\\w+\\s*(=|;)", "MEDIUM",
                    "Public field",
                    "Make fields private and use getters/setters so the class controls its own data."),

            rule("js", "\\bvar\\s+\\w+", "LOW",
                    "var used",
                    "Use let or const. var has confusing scope rules."),
            rule("js", "[^=!<>]==[^=]", "MEDIUM",
                    "Loose equality ==",
                    "Use === so JavaScript does not silently change types before comparing."),
            rule("js", "console\\.log\\s*\\(", "LOW",
                    "console.log left in",
                    "Remove debug prints before sharing the project."),
            rule("js", "\\beval\\s*\\(", "HIGH",
                    "eval() used",
                    "eval runs any text as code. It is a big security hole; find another way."),
            rule("js", "\\.innerHTML\\s*=|document\\.write\\s*\\(", "HIGH",
                    "innerHTML / document.write with data (XSS risk)",
                    "Use textContent for plain text. Putting user text into innerHTML lets attackers run scripts."),
            rule("js", "catch\\s*\\([^)]*\\)\\s*\\{\\s*\\}", "HIGH",
                    "Empty catch block",
                    "An empty catch hides errors. At least show or log the problem."),

            rule("python", "except\\s*:", "HIGH",
                    "Bare except",
                    "Write except SomeError: so you do not also swallow Ctrl+C and real bugs."),
            rule("python", "\\b(eval|exec)\\s*\\(", "HIGH",
                    "eval() / exec() used",
                    "These run any text as code. Avoid them, especially with user input."),
            rule("python", "def\\s+\\w+\\s*\\([^)]*=\\s*(\\[\\]|\\{\\})", "MEDIUM",
                    "Mutable default argument",
                    "A default of [] or {} is shared between calls. Use None and create the list inside the function."),
            rule("python", "from\\s+\\w+\\s+import\\s+\\*", "LOW",
                    "import * used",
                    "Import only the names you need so it is clear where each one comes from."),
            rule("python", "^\\s*print\\s*\\(", "LOW",
                    "print() left in",
                    "Use the logging module for real programs.")
    );

    /** Look at the code and guess the language from typical words. */
    public String detectLanguage(String code) {
        String c = code.toLowerCase(Locale.ROOT);
        if (c.contains("public class ") || c.contains("system.out") || c.contains("import java.") || c.contains("@override")) return "java";
        if (c.contains("def ") && c.contains(":") && !c.contains("function")) return "python";
        if (c.contains("import ") && c.contains(" from ") && c.contains("def ")) return "python";
        // JavaScript hints. "var " and "document." are here so old-style code (only var, no const/let) is still recognised.
        if (c.contains("function ") || c.contains("const ") || c.contains("=>") || c.contains("console.") || c.contains("let ")
                || c.contains("var ") || c.contains("document.") || c.contains("window.") || c.contains("===") || c.contains("eval(")) return "js";
        if (c.contains("<html") || c.contains("<div")) return "html";
        return "unknown";
    }

    public Review review(String code) {
        String lang = detectLanguage(code);
        String[] lines = code.split("\\R", -1);
        List<Finding> found = new ArrayList<>();

        // 1) Line-by-line rules. For each rule I only report the first 3 matches so the list stays readable.
        for (Rule r : RULES) {
            if (!r.lang().equals("any") && !r.lang().equals(lang)) continue;
            int reported = 0;
            for (int i = 0; i < lines.length && reported < 3; i++) {
                if (r.pattern().matcher(lines[i]).find()) {
                    found.add(new Finding(r.severity(), i + 1, r.title(), r.tip()));
                    reported++;
                }
            }
        }

        // 2) Whole-file checks: very long lines, and almost no comments in a long file.
        for (int i = 0, longLines = 0; i < lines.length && longLines < 2; i++) {
            if (lines[i].length() > 140) {
                found.add(new Finding("LOW", i + 1, "Very long line",
                        "Break it into shorter lines (under about 100 characters) so it is easy to read."));
                longLines++;
            }
        }
        int nonEmpty = (int) java.util.Arrays.stream(lines).filter(l -> !l.isBlank()).count();
        long commentLines = java.util.Arrays.stream(lines).filter(l -> {
            String t = l.trim();
            return t.startsWith("//") || t.startsWith("#") || t.startsWith("/*") || t.startsWith("*");
        }).count();
        if (nonEmpty >= 30 && commentLines == 0) {
            found.add(new Finding("LOW", 0, "No comments at all",
                    "Add short comments that explain WHY a part exists. Recruiters read comments."));
        }

        // Most serious first, then by line number
        found.sort((a, b) -> {
            int s = Integer.compare(rank(a.severity()), rank(b.severity()));
            return s != 0 ? s : Integer.compare(a.line(), b.line());
        });

        // Score: start at 100 and take points off for every problem
        int score = 100;
        for (Finding f : found) score -= switch (f.severity()) { case "HIGH" -> 15; case "MEDIUM" -> 7; default -> 3; };
        return new Review(Math.max(0, score), lang, lines.length, found);
    }

    private static int rank(String severity) {
        return switch (severity) { case "HIGH" -> 0; case "MEDIUM" -> 1; default -> 2; };
    }
}
