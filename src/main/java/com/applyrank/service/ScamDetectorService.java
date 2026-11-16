package com.applyrank.service;

import com.applyrank.service.CodeReviewService.Finding;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scam Detector - the "rules" part.
 * It reads a job offer message and looks for the warning signs that fake offers usually have
 * (asking for money, asking for bank details, WhatsApp-only chat, "no interview" and so on).
 * The result is a RISK number: the higher it is, the more suspicious the offer is.
 * Important: this can only show warning signs. It can never prove that an offer is real.
 */
@Service
public class ScamDetectorService {

    /** risk = 0..100 (higher = more suspicious), level = LOW / MEDIUM / HIGH, lines = how many lines the message has */
    public record Result(int score, String level, int lines, List<Finding> findings) {}

    private record Rule(Pattern pattern, String severity, String title, String tip) {}

    private static Rule rule(String regex, String sev, String title, String tip) {
        return new Rule(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), sev, title, tip);
    }

    private static final List<Rule> RULES = List.of(
            // ---- HIGH: the classic signs of a scam ----
            rule("(registration|processing|security|training|joining|refundable|application|verification|kit|laptop|interview)\\s+(fee|fees|charges?|deposit|amount)",
                    "HIGH", "Asks you to pay money",
                    "A real employer never takes money from a candidate, not even a 'refundable' deposit. Do not pay anything."),
            rule("\\b(otp|cvv|atm\\s*pin|net\\s*banking|upi\\s*pin|bank\\s+(account\\s+)?(details|number|password)|debit\\s*card|credit\\s*card)\\b",
                    "HIGH", "Asks for bank, card or OTP details",
                    "Never share an OTP, PIN or card number with a recruiter. Salary details are only needed after you have joined."),
            rule("(cheque|check)[^.\\n]{0,40}(deposit|send|forward|transfer)|buy\\s+(a\\s+)?(laptop|equipment|software|gift\\s*cards?)\\s+(first|yourself|from)",
                    "HIGH", "Fake cheque / buy equipment first trick",
                    "Scammers send a fake cheque and ask you to send part of it back. Real companies give you equipment themselves."),
            rule("\\b(anydesk|teamviewer|quick\\s*support)\\b|\\.apk\\b",
                    "HIGH", "Asks you to install remote-control software or an APK",
                    "This gives the scammer control of your phone or laptop. Never install it for a job."),
            rule("\\b(bitcoin|crypto(currency)?|usdt|gift\\s*cards?)\\b",
                    "HIGH", "Mentions crypto or gift cards",
                    "Real jobs do not pay or collect money in crypto or gift cards."),
            rule("(like|liking|rate|rating|review|reviewing|subscribe)\\s+(youtube|videos?|products?|posts?|channels?)",
                    "HIGH", "Paid for liking, rating or reviewing",
                    "'Earn money by liking videos' is a very common task scam. It starts small and then asks you to deposit money."),

            // ---- MEDIUM: suspicious, check carefully ----
            rule("\\b(whatsapp|telegram|signal)\\b",
                    "MEDIUM", "Recruiter wants to talk only on WhatsApp / Telegram",
                    "Real recruiters also use official company email. Ask for an email from the company's own domain."),
            rule("[\\w.+-]+@(gmail|yahoo|outlook|hotmail|rediffmail|proton(mail)?)\\.\\w+",
                    "MEDIUM", "'HR' uses a free personal email address",
                    "Companies use their own email domain (name@company.com). A free email for a real company's HR is a warning sign."),
            rule("no\\s+interview|without\\s+(an\\s+)?interview|direct\\s+(joining|selection)|instant\\s+(joining|selection|offer)|selected\\s+without",
                    "MEDIUM", "Job offered without an interview",
                    "Real companies always test or interview you first. An offer without any interview is suspicious."),
            rule("(earn|earning|income|make|salary|pay)[^.\\n]{0,40}(per|a|/)\\s*(day|daily|week|weekly|hour)",
                    "MEDIUM", "Very high pay promised per day / week",
                    "Compare with the normal salary for that role. If it sounds too good to be true, it usually is."),
            rule("guaranteed\\s+(job|placement|selection|salary|income)|100\\s*%\\s*(job|placement|guarantee)",
                    "MEDIUM", "Guaranteed job or placement",
                    "No honest company or institute can guarantee a job. Be careful."),
            rule("limited\\s+(seats|slots|vacancies|positions)|last\\s+chance|today\\s+only|act\\s+now|within\\s+\\d+\\s+(hours|hrs|minutes)",
                    "MEDIUM", "Pressure to decide very fast",
                    "Scammers rush you so you do not check. Take your time and verify first."),
            rule("bit\\.ly|tinyurl|cutt\\.ly|rb\\.gy|t\\.me/|wa\\.me/|goo\\.gl",
                    "MEDIUM", "Shortened or chat links",
                    "A short link hides where it really goes. Ask for the company's real website."),
            rule("\\b(aadhaar|aadhar|pan\\s*card|passport\\s*copy|marksheet)\\b",
                    "MEDIUM", "Asks for ID documents very early",
                    "Real employers ask for ID documents only after a written offer and a proper joining process."),

            // ---- LOW: small hints ----
            rule("data\\s+entry|typing\\s+job|form\\s+filling|copy[- ]?paste",
                    "LOW", "Easy 'data entry / typing' work from home",
                    "These are the most copied scam job types. Check the company very carefully."),
            rule("\\burgent(ly)?\\b",
                    "LOW", "Says 'urgent'",
                    "Urgent hiring can be real, but combine it with the other signs before you trust it.")
    );

    public Result check(String text) {
        List<Finding> found = new ArrayList<>();
        for (Rule r : RULES) {
            Matcher m = r.pattern().matcher(text);
            if (m.find()) {
                // I work out the line number where the sign was found, so the user can see it in their message
                int line = 1;
                for (int i = 0; i < m.start(); i++) if (text.charAt(i) == '\n') line++;
                found.add(new Finding(r.severity(), line, r.title(), r.tip()));
            }
        }
        // A message with almost no details is also a bit suspicious (no company name, no role, no address)
        if (text.strip().length() < 150) {
            found.add(new Finding("LOW", 0, "Very little information in the message",
                    "A real offer names the company, the role, the location and who to contact. Ask for these details."));
        }

        found.sort((a, b) -> {
            int s = Integer.compare(rank(a.severity()), rank(b.severity()));
            return s != 0 ? s : Integer.compare(a.line(), b.line());
        });

        // Risk: every sign adds points. HIGH signs add the most.
        int risk = 0;
        for (Finding f : found) risk += switch (f.severity()) { case "HIGH" -> 30; case "MEDIUM" -> 15; default -> 6; };
        risk = Math.min(100, risk);
        String level = risk >= 60 ? "HIGH" : risk >= 30 ? "MEDIUM" : "LOW";
        return new Result(risk, level, text.split("\\R", -1).length, found);
    }

    private static int rank(String severity) {
        return switch (severity) { case "HIGH" -> 0; case "MEDIUM" -> 1; default -> 2; };
    }
}
