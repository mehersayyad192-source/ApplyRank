package com.applyrank.service;

import com.applyrank.service.CodeReviewService.Finding;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Page Auditor - the "rules" part.
 * The user pastes the HTML of their portfolio page. I read it with Jsoup (a library that understands HTML)
 * and check the things recruiters, search engines and phones care about: title, mobile settings, headings,
 * image descriptions, links, forms and so on.
 *
 * Safety notes:
 *  - Jsoup only READS the HTML. It never runs scripts, so a pasted page cannot do anything.
 *  - I do NOT fetch web addresses. Opening any address the user types would let someone make my server visit
 *    private places (this attack is called SSRF). Pasting the HTML is safer and works for pages not online yet.
 */
@Service
public class PageAuditService {

    /** score = 100 minus points for problems, summary = short facts about the page (sent to the AI instead of the whole page) */
    public record Result(int score, int lines, List<Finding> findings, String summary) {}

    private static final Pattern DOCTYPE = Pattern.compile("^\\s*(<!--.*?-->\\s*)*<!doctype\\s+html", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern VAGUE_LINK = Pattern.compile("^(click here|here|read more|more|link|this)$", Pattern.CASE_INSENSITIVE);

    public Result audit(String html) {
        Document doc = Jsoup.parse(html);
        List<Finding> f = new ArrayList<>();

        // ---------- basics every page needs ----------
        if (!DOCTYPE.matcher(html).find())
            f.add(new Finding("MEDIUM", 0, "Missing <!DOCTYPE html>", "Put <!DOCTYPE html> on the very first line. Without it, browsers show the page in old 'quirks mode'."));
        if (doc.selectFirst("html[lang]") == null)
            f.add(new Finding("MEDIUM", 0, "No language set on <html>", "Write <html lang=\"en\">. Screen readers and Google use it to read your page correctly."));

        String title = doc.title().strip();
        if (title.isEmpty())
            f.add(new Finding("HIGH", 0, "Missing <title>", "Add a clear <title>, for example 'Your Name - Java Developer'. It is the first thing a recruiter sees in the browser tab and in Google."));
        else if (title.length() < 10 || title.length() > 60)
            f.add(new Finding("LOW", 0, "Title length is not ideal (" + title.length() + " characters)", "Keep the title between about 10 and 60 characters so Google shows all of it."));

        Element desc = doc.selectFirst("meta[name=description]");
        if (desc == null || desc.attr("content").isBlank())
            f.add(new Finding("MEDIUM", 0, "Missing meta description", "Add <meta name=\"description\" content=\"...\"> with one sentence about you. Google shows it under your link."));
        if (doc.selectFirst("meta[name=viewport]") == null)
            f.add(new Finding("HIGH", 0, "Missing mobile viewport tag", "Add <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">. Without it your page looks tiny and broken on phones."));
        if (doc.selectFirst("meta[property=og:title]") == null)
            f.add(new Finding("LOW", 0, "No Open Graph tags (link preview)", "Add og:title and og:description meta tags so your link shows a nice preview on LinkedIn and WhatsApp."));
        if (doc.selectFirst("link[rel~=(?i)icon]") == null)
            f.add(new Finding("LOW", 0, "No favicon", "Add a small icon with <link rel=\"icon\" ...> so your tab looks professional."));

        // ---------- headings ----------
        Elements h1 = doc.select("h1");
        if (h1.isEmpty()) f.add(new Finding("MEDIUM", 0, "No <h1> heading", "Every page needs one main <h1>, usually your name and role."));
        else if (h1.size() > 1) f.add(new Finding("LOW", 0, h1.size() + " <h1> headings", "Use only one <h1> per page. Use <h2> and <h3> for the smaller sections."));
        int prev = 0;
        for (Element h : doc.select("h1, h2, h3, h4, h5, h6")) {
            int level = h.tagName().charAt(1) - '0';
            if (prev != 0 && level > prev + 1) {
                f.add(new Finding("LOW", 0, "Heading levels skip (<h" + prev + "> then <h" + level + ">)", "Do not jump over levels. Go h1, then h2, then h3, so the page outline makes sense."));
                break;                                   // one example is enough
            }
            prev = level;
        }

        // ---------- images ----------
        Elements imgs = doc.select("img");
        long noAlt = imgs.stream().filter(i -> !i.hasAttr("alt")).count();
        if (noAlt > 0)
            f.add(new Finding("MEDIUM", 0, noAlt + " image(s) without alt text", "Add alt=\"short description\" to each image. Blind users and Google depend on it. Use alt=\"\" only for pure decoration."));
        long noSize = imgs.stream().filter(i -> !(i.hasAttr("width") && i.hasAttr("height"))).count();
        if (noSize > 0)
            f.add(new Finding("LOW", 0, noSize + " image(s) without width and height", "Set width and height so the page does not jump around while images load."));

        // ---------- links and buttons ----------
        long emptyLinks = doc.select("a[href]").stream().filter(a -> a.text().isBlank() && a.select("img[alt]").isEmpty() && !a.hasAttr("aria-label")).count();
        if (emptyLinks > 0)
            f.add(new Finding("MEDIUM", 0, emptyLinks + " link(s) with no readable text", "Every link needs text (or an aria-label) so people know where it goes."));
        long vague = doc.select("a[href]").stream().filter(a -> VAGUE_LINK.matcher(a.text().strip()).matches()).count();
        if (vague > 0)
            f.add(new Finding("LOW", 0, vague + " link(s) say 'click here' / 'read more'", "Write what the link is about, for example 'View my weather app project'."));
        long unsafeBlank = doc.select("a[target=_blank]").stream().filter(a -> !a.attr("rel").toLowerCase().contains("noopener")).count();
        if (unsafeBlank > 0)
            f.add(new Finding("MEDIUM", 0, unsafeBlank + " link(s) open a new tab without rel=\"noopener\"", "Add rel=\"noopener noreferrer\" so the new page cannot control your page."));
        long http = doc.select("a[href^=http://], img[src^=http://], script[src^=http://], link[href^=http://]").size();
        if (http > 0)
            f.add(new Finding("LOW", 0, http + " insecure http:// address(es)", "Use https:// everywhere, otherwise browsers show warnings and block some content."));
        long emptyButtons = doc.select("button").stream().filter(b -> b.text().isBlank() && !b.hasAttr("aria-label") && b.select("img[alt]").isEmpty()).count();
        if (emptyButtons > 0)
            f.add(new Finding("MEDIUM", 0, emptyButtons + " button(s) with no text", "Give each button text or an aria-label so everyone knows what it does."));

        // ---------- forms ----------
        long noLabel = doc.select("input, textarea, select").stream().filter(this::needsLabel).filter(e -> !hasLabel(doc, e)).count();
        if (noLabel > 0)
            f.add(new Finding("MEDIUM", 0, noLabel + " form field(s) without a label", "Connect each field to a <label for=\"id\">. Placeholders alone are not enough."));

        // ---------- layout and speed ----------
        if (doc.select("main, header, nav, footer, section, article").isEmpty())
            f.add(new Finding("LOW", 0, "No meaningful layout tags", "Use <header>, <nav>, <main>, <section> and <footer> instead of only <div>. It helps screen readers and Google."));
        long blocking = doc.select("head script[src]").stream().filter(s -> !s.hasAttr("defer") && !s.hasAttr("async") && !"module".equals(s.attr("type"))).count();
        if (blocking > 0)
            f.add(new Finding("LOW", 0, blocking + " script(s) in <head> that block the page", "Add the defer attribute so the page shows up before the scripts finish loading."));
        long inline = doc.select("[style]").size();
        if (inline > 10)
            f.add(new Finding("LOW", 0, inline + " inline style attributes", "Move styles into a CSS file. It is easier to change and keeps your HTML clean."));
        if (!doc.select("font, center, marquee, blink").isEmpty())
            f.add(new Finding("LOW", 0, "Old HTML tags used (font, center, marquee...)", "These tags are outdated. Use CSS instead."));

        // ---------- what a recruiter looks for in a portfolio ----------
        List<String> missing = new ArrayList<>();
        if (doc.select("a[href*=github.com]").isEmpty()) missing.add("GitHub");
        if (doc.select("a[href*=linkedin.com]").isEmpty()) missing.add("LinkedIn");
        if (doc.select("a[href^=mailto:]").isEmpty()) missing.add("email");
        if (!missing.isEmpty())
            f.add(new Finding("LOW", 0, "Portfolio has no link to: " + String.join(", ", missing), "Recruiters want to reach you and see your code in one click. Add these links near the top."));

        f.sort((a, b) -> {
            int s = Integer.compare(rank(a.severity()), rank(b.severity()));
            return s != 0 ? s : a.title().compareTo(b.title());
        });
        int score = 100;
        for (Finding x : f) score -= switch (x.severity()) { case "HIGH" -> 15; case "MEDIUM" -> 7; default -> 3; };

        // A short summary: this (and not the full page) is what goes to the AI if the user asks for AI advice
        StringBuilder sum = new StringBuilder();
        sum.append("Title: ").append(title.isEmpty() ? "(none)" : title).append('\n');
        sum.append("Headings: ");
        doc.select("h1, h2, h3").stream().limit(12).forEach(h -> sum.append(h.tagName()).append('=').append(cut(h.text(), 60)).append("; "));
        sum.append("\nCounts: ").append(imgs.size()).append(" images, ").append(doc.select("a[href]").size()).append(" links, ")
                .append(doc.select("form").size()).append(" forms, ").append(doc.select("script").size()).append(" scripts");
        return new Result(Math.max(0, score), html.split("\\R", -1).length, f, sum.toString());
    }

    private boolean needsLabel(Element e) {
        String type = e.attr("type").toLowerCase();
        return !(type.equals("hidden") || type.equals("submit") || type.equals("button") || type.equals("reset") || type.equals("image"));
    }

    /** A field has a label if: aria-label is set, or a <label for=id> exists, or it sits inside a <label>. */
    private boolean hasLabel(Document doc, Element e) {
        if (e.hasAttr("aria-label") || e.hasAttr("aria-labelledby")) return true;
        // I compare the text myself (instead of building a CSS selector) so a strange id can never break the check
        if (!e.id().isEmpty() && doc.select("label").stream().anyMatch(l -> l.attr("for").equals(e.id()))) return true;
        return e.parents().stream().anyMatch(p -> p.tagName().equals("label"));
    }

    private static String cut(String s, int n) { return s.length() <= n ? s : s.substring(0, n) + "..."; }

    private static int rank(String severity) {
        return switch (severity) { case "HIGH" -> 0; case "MEDIUM" -> 1; default -> 2; };
    }
}
