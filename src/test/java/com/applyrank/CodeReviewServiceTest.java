package com.applyrank;

import com.applyrank.service.CodeReviewService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Checks that my review rules catch the common mistakes (no Spring needed, so this runs fast). */
class CodeReviewServiceTest {
    private final CodeReviewService reviewer = new CodeReviewService();

    private boolean has(CodeReviewService.Review r, String titlePart) {
        return r.findings().stream().anyMatch(f -> f.title().toLowerCase().contains(titlePart.toLowerCase()));
    }

    @Test
    void detectsLanguages() {
        assertEquals("java", reviewer.detectLanguage("public class A { }"));
        assertEquals("js", reviewer.detectLanguage("const a = () => 1;"));
        assertEquals("python", reviewer.detectLanguage("def f(x):\n    return x"));
    }

    @Test
    void oldStyleJavascriptWithOnlyVarIsRecognised() {
        // This is the exact code that once came back as "unknown" with no findings
        var r = reviewer.review("var a = 1;\nif (a == 2) { eval(a); }");
        assertEquals("js", r.language());
        assertTrue(has(r, "eval"));
        assertTrue(has(r, "loose equality"));
    }

    @Test
    void javaProblemsAreFound() {
        String code = """
                public class Demo {
                    public String name;
                    void run() {
                        String password = "hunter2hunter2";
                        try { work(); } catch (Exception e) { }
                        if (name == "bob") System.out.println("hi");
                        String q = "SELECT * FROM users WHERE id=" + id;
                    }
                }
                """;
        var r = reviewer.review(code);
        assertTrue(has(r, "secret"));
        assertTrue(has(r, "empty catch"));
        assertTrue(has(r, "== or !="));
        assertTrue(has(r, "sql"));
        assertTrue(has(r, "public field"));
        assertTrue(r.score() < 60, "many problems must give a low score, got " + r.score());
        assertEquals("HIGH", r.findings().get(0).severity(), "most serious problem must be listed first");
    }

    @Test
    void javascriptProblemsAreFound() {
        var r = reviewer.review("var a = 1;\nif (a == 2) { console.log(a); }\nel.innerHTML = x;\neval(y);");
        assertTrue(has(r, "var used"));
        assertTrue(has(r, "loose equality"));
        assertTrue(has(r, "innerHTML"));
        assertTrue(has(r, "eval"));
    }

    @Test
    void strictEqualsIsNotFlagged() {
        var r = reviewer.review("const a = 1;\nif (a === 2) { return a !== 3; }");
        assertFalse(has(r, "loose equality"));
    }

    @Test
    void cleanCodeScoresHigh() {
        var r = reviewer.review("// adds two numbers\nconst add = (a, b) => a + b;\n");
        assertEquals(100, r.score());
        assertTrue(r.findings().isEmpty());
    }

    @Test
    void pythonProblemsAreFound() {
        var r = reviewer.review("def f(x=[]):\n    try:\n        pass\n    except:\n        print('x')\n");
        assertTrue(has(r, "mutable default"));
        assertTrue(has(r, "bare except"));
    }
}
