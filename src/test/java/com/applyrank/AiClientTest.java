package com.applyrank;

import com.applyrank.service.AiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests AiClient WITHOUT a real API key: I start a tiny fake "Gemini" server on my own computer
 * and check that AiClient sends the right request and reads the streamed answer correctly.
 */
class AiClientTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void noKeyMeansDisabled() {
        assertFalse(new AiClient("", "m", "http://localhost:1", json).enabled());
        assertFalse(new AiClient("   ", "m", "http://localhost:1", json).enabled());
        assertTrue(new AiClient("abc", "m", "http://localhost:1", json).enabled());
    }

    @Test
    void readsTextPiecesAndSkipsThoughtsAndJunk() {
        AiClient c = new AiClient("k", "m", "http://localhost:1", json);
        assertEquals("Hello", c.textOfLine("data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello\"}]}}]}"));
        assertEquals("", c.textOfLine("data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"secret thinking\",\"thought\":true}]}}]}"));
        assertNull(c.textOfLine(""));
        assertNull(c.textOfLine(": keep-alive comment"));
        assertNull(c.textOfLine("data: {not json"));
    }

    @Test
    void streamsFromFakeServerAndSendsKeyInHeaderNotUrl() throws Exception {
        AtomicReference<String> keyHeader = new AtomicReference<>(), path = new AtomicReference<>(), body = new AtomicReference<>();
        HttpServer fake = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fake.createContext("/", ex -> {
            keyHeader.set(ex.getRequestHeaders().getFirst("x-goog-api-key"));
            path.set(ex.getRequestURI().toString());
            body.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            String sse = "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Part one. \"}]}}]}\n\n"
                       + "data: {\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Part two.\"}]}}]}\n\n";
            ex.getResponseHeaders().add("Content-Type", "text/event-stream");
            ex.sendResponseHeaders(200, 0);
            try (OutputStream o = ex.getResponseBody()) { o.write(sse.getBytes(StandardCharsets.UTF_8)); }
        });
        fake.start();
        try {
            AiClient c = new AiClient("my-secret-key", "test-model", "http://127.0.0.1:" + fake.getAddress().getPort() + "/v1beta", json);
            List<String> got = new ArrayList<>();
            c.stream("be nice", "review this", got::add);
            assertEquals(List.of("Part one. ", "Part two."), got);
            assertEquals("my-secret-key", keyHeader.get());
            assertFalse(path.get().contains("my-secret-key"), "the key must never be in the web address");
            assertTrue(path.get().contains("test-model:streamGenerateContent?alt=sse"), path.get());
            assertTrue(body.get().contains("system_instruction") && body.get().contains("review this"), body.get());
        } finally { fake.stop(0); }
    }

    @Test
    void errorFromServerBecomesIOExceptionWithoutLeakingKey() throws Exception {
        HttpServer fake = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        fake.createContext("/", ex -> { ex.sendResponseHeaders(429, -1); ex.close(); });
        fake.start();
        try {
            AiClient c = new AiClient("my-secret-key", "m", "http://127.0.0.1:" + fake.getAddress().getPort(), json);
            IOException e = assertThrows(IOException.class, () -> c.stream("s", "u", t -> {}));
            assertTrue(e.getMessage().contains("429"));
            assertFalse(e.getMessage().contains("my-secret-key"));
        } finally { fake.stop(0); }
    }
}
