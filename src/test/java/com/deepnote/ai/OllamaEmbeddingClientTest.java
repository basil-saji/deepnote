package com.deepnote.ai;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class OllamaEmbeddingClientTest {
    @Test void sendsEmbeddingGemmaRetrievalPromptsAndEscapesJsonControls() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        String values = "0,".repeat(767) + "0";
        server.createContext("/api/embed", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = ("{\"embeddings\":[[" + values + "]]}" ).getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length); exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            var client = new OllamaEmbeddingClient(base, "embeddinggemma:300m", Duration.ofSeconds(5), 768);
            client.embedQuery("line1\nline2\t\"quoted\"\\path");
            assertTrue(requestBody.get().contains("task: search result | query: line1\\nline2\\t\\\"quoted\\\"\\\\path"));
            client.embedDocument("A title", "document text");
            assertTrue(requestBody.get().contains("title: A title | text: document text"));
        } finally { server.stop(0); }
    }
}
