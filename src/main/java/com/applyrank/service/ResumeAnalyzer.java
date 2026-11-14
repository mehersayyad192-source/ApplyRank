package com.applyrank.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Honest, explainable resume check: extracts the text (PDF or DOCX) and scores it with transparent rules
 * (keyword coverage for the chosen track, quantified results, links, sensible length). It is a heuristic,
 * NOT an AI judgement - the UI explains what matched and what is missing.
 */
@Service
public class ResumeAnalyzer {
    public record Analysis(int score, List<String> matched, List<String> missing, List<String> tips) {}

    private static final Map<String, List<String>> KEYWORDS = Map.of(
            "frontend", List.of("html", "css", "javascript", "react", "typescript", "responsive", "git", "accessibility", "testing", "api"),
            "backend", List.of("java", "spring", "sql", "rest", "api", "docker", "git", "jpa", "testing", "security"),
            "data", List.of("sql", "python", "pandas", "excel", "tableau", "statistics", "etl", "dashboard", "git", "analysis"),
            "fullstack", List.of("java", "react", "spring", "sql", "rest", "docker", "git", "javascript", "testing", "api"));

    public String extractText(byte[] data, String filename) throws IOException {
        if (data.length < 8) throw new IllegalArgumentException("The file looks empty.");
        boolean pdf = data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F';
        boolean zip = data[0] == 'P' && data[1] == 'K';
        if (pdf) {
            try (PDDocument doc = Loader.loadPDF(data)) {
                return new PDFTextStripper().getText(doc);
            }
        }
        if (zip && filename != null && filename.toLowerCase().endsWith(".docx")) {
            try (ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(data))) {
                ZipEntry e;
                while ((e = z.getNextEntry()) != null) {
                    if (e.getName().equals("word/document.xml")) {
                        String xml = new String(z.readAllBytes(), StandardCharsets.UTF_8);
                        return xml.replaceAll("</w:p>", "\n").replaceAll("<[^>]+>", " ");
                    }
                }
            }
        }
        throw new IllegalArgumentException("Please upload a PDF or DOCX file.");
    }

    public Analysis analyze(String text, String track) {
        String t = text == null ? "" : text.toLowerCase();
        List<String> kws = KEYWORDS.getOrDefault(track == null ? "" : track.toLowerCase(), KEYWORDS.get("fullstack"));
        List<String> matched = new ArrayList<>(), missing = new ArrayList<>(), tips = new ArrayList<>();
        for (String k : kws) {
            boolean hit = Pattern.compile("\\b" + Pattern.quote(k) + "\\b").matcher(t).find();
            (hit ? matched : missing).add(k);
        }
        int score = (int) Math.round(55.0 * matched.size() / kws.size());   // up to 55 for keywords

        boolean metrics = Pattern.compile("\\d+\\s*(%|\\+|x\\b|users|ms|k\\b)").matcher(t).find();
        if (metrics) score += 15; else tips.add("Add numbers to your achievements (e.g. 'cut load time by 40%').");

        boolean links = t.contains("github.com") || t.contains("linkedin.com");
        if (links) score += 10; else tips.add("Add your GitHub / LinkedIn link.");

        int words = t.isBlank() ? 0 : t.trim().split("\\s+").length;
        if (words >= 150 && words <= 900) score += 10;
        else tips.add(words < 150 ? "Your resume looks very short - add projects and skills." : "Your resume is long - keep it to one page.");

        boolean sections = t.contains("project") && t.contains("education") && t.contains("skill");
        if (sections) score += 10; else tips.add("Use clear sections: Education, Skills, Projects.");

        if (!missing.isEmpty()) tips.add("Missing keywords for this track: " + String.join(", ", missing.subList(0, Math.min(4, missing.size()))) + ".");
        return new Analysis(Math.max(0, Math.min(100, score)), matched, missing, tips);
    }
}
