package com.applyrank.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends "click this link" e-mails.
 *  - SMTP configured (spring.mail.host set)  -> a real e-mail is sent.
 *  - nothing configured (default, local dev) -> the link is printed in the backend console instead, so you can still test.
 */
@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final ObjectProvider<JavaMailSender> mail;
    private final String from;
    private final String frontendUrl;
    private final Map<String, String> lastLink = new ConcurrentHashMap<>();     // dev mode only (used by tests)

    public EmailService(ObjectProvider<JavaMailSender> mail,
                        @Value("${app.mail.from:no-reply@applyrank.dev}") String from,
                        @Value("${app.frontend-url:http://127.0.0.1:5500}") String frontendUrl) {
        this.mail = mail; this.from = from; this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    public void sendLink(String to, String subject, String intro, String page, String token) {
        String link = frontendUrl + "/" + page + "?token=" + token;
        JavaMailSender sender = mail.getIfAvailable();
        if (sender == null) {
            lastLink.put(to, link);
            log.warn("DEV MODE (no SMTP set). E-mail to {} | {} | open this link: {}", to, subject, link);
            return;
        }
        try {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setFrom(from);
            m.setTo(to);
            m.setSubject(subject);
            m.setText(intro + "\n\n" + link + "\n\nIf you did not ask for this, you can ignore this e-mail.");
            sender.send(m);
        } catch (Exception e) {
            log.error("Could not send e-mail to {}: {}", to, e.getMessage());      // never break the request because of mail
        }
    }

    public String lastLink(String to) { return lastLink.get(to); }
}
