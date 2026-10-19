package com.deepnote.ai;

import com.deepnote.exception.ChatModelException;
import com.deepnote.model.ChatMessage;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/** Minimal Ollama /api/chat client using only the JDK HTTP client. */
public class OllamaChatModelClient implements ChatModelClient {
    private final String endpoint;
    private final String model;
    private final HttpClient httpClient;
    private final Duration timeout;

    public OllamaChatModelClient(String baseUrl, String model, Duration timeout) {
        this.endpoint = baseUrl.replaceAll("/$", "") + "/api/chat";
        this.model = model;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override public String generate(String systemInstruction, List<ChatMessage> messages) {
        StringBuilder json = new StringBuilder("{\"model\":\"").append(escape(model)).append("\",\"stream\":false,\"messages\":[");
        json.append("{\"role\":\"system\",\"content\":\"").append(escape(systemInstruction)).append("\"}");
        for (ChatMessage message : messages) {
            json.append(",{");
            json.append("\"role\":\"").append(message.role() == ChatMessage.Role.USER ? "user" : "assistant").append("\",\"content\":\"").append(escape(message.content())).append("\"}");
        }
        json.append("]}");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(timeout)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.toString())).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) throw new ChatModelException("Ollama returned HTTP " + response.statusCode() + ". Is model '" + model + "' installed?");
            String content = extractJsonString(response.body(), "content");
            if (content == null) throw new ChatModelException("Ollama returned an unexpected response.");
            return content;
        } catch (ChatModelException exception) { throw exception; }
        catch (InterruptedException exception) { Thread.currentThread().interrupt(); throw new ChatModelException("The Ollama request was interrupted.", exception); }
        catch (Exception exception) { throw new ChatModelException("Could not reach Ollama at " + endpoint + ". Check that it is running and the model is installed.", exception); }
    }

    private String extractJsonString(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return null;
        start += marker.length();
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char character = json.charAt(i);
            if (escaped) {
                result.append(switch (character) { case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t'; default -> character; });
                escaped = false;
                continue;
            }
            if (character == '\\') { escaped = true; continue; }
            if (character == '"') return result.toString();
            result.append(character);
        }
        return null;
    }

    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n"); }
}
