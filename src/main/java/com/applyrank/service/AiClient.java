package com.applyrank.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The one place that talks to the AI (Google Gemini).
 * Every AI Lab tool (code review, scam detector, page auditor) will use this same class.
 *
 * Rules I follow here:
 *  - the API key is read from the server settings only, and is never sent to the browser or written in logs
 *  - the answer is "streamed": I get it in small pieces and pass each piece on straight away
 *  - if no key is set, enabled() is false and the tools simply skip the AI part
 */
@Service
public class AiClient {
    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public AiClient(@Value("${app.ai.api-key:}") String apiKey,
                    @Value("${app.ai.model:gemini-3.8-flash}") String model,
                    @Value("${app.ai.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
                    ObjectMapper json) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.baseUrl = baseUrl;
        this.json = json;
    }

    /** true only when an API key was provided */
    public boolean enabled() {
        return !apiKey.isEmpty();
    }

    /**
     * Sends the instructions + the user's text to the AI and calls onText() for every piece of the answer.
     * This method waits until the whole answer has arrived.
     */
    public void stream(String systemInstruction, String userText, Consumer<String> onText) throws IOException {
        if (!enabled()) throw new IOException("AI is not configured");

        // The request body Gemini expects: instructions, the user message, and a few settings
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", userText)))),
                "generationConfig", Map.of("maxOutputTokens", 900, "temperature", 0.3));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/models/" + model + ":streamGenerateContent?alt=sse"))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)            // the key goes in a header, not in the web address
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                .build();

        try {
            HttpResponse<Stream<String>> res = http.send(req, HttpResponse.BodyHandlers.ofLines());
            if (res.statusCode() != 200) {
                res.body().close();
                // I log only the number, never the key or the user's text
                log.warn("AI request failed with HTTP {}", res.statusCode());
                throw new IOException("The AI service is busy or not available right now (HTTP " + res.statusCode() + ").");
            }
            try (Stream<String> lines = res.body()) {
                // Streamed answers come as lines like:  data: {json}
                lines.forEach(line -> {
                    String piece = textOfLine(line);
                    if (piece != null && !piece.isEmpty()) onText.accept(piece);
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("AI request was interrupted");
        }
    }

    /** Pulls the text out of one "data: {...}" line. Returns null for lines that carry no text. */
    public String textOfLine(String line) {
        if (line == null || !line.startsWith("data:")) return null;
        String payload = line.substring(5).trim();
        if (payload.isEmpty()) return null;
        try {
            JsonNode parts = json.readTree(payload).path("candidates").path(0).path("content").path("parts");
            StringBuilder sb = new StringBuilder();
            for (JsonNode p : parts) {
                if (p.path("thought").asBoolean(false)) continue;   // skip the AI's private "thinking" parts
                sb.append(p.path("text").asText(""));
            }
            return sb.toString();
        } catch (IOException e) {
            return null;                                           // a broken line is just ignored
        }
    }
}
