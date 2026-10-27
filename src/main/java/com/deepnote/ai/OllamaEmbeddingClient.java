package com.deepnote.ai;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

/** Local-only Ollama embeddings. The configured dimension is enforced exactly. */
public final class OllamaEmbeddingClient {
    private final String baseUrl, model; private final Duration timeout; private final int dimensions;
    public OllamaEmbeddingClient(String baseUrl, String model, Duration timeout, int dimensions) { this.baseUrl=baseUrl; this.model=model; this.timeout=timeout; this.dimensions=dimensions; }
    public List<Float> embed(String input) throws Exception {
        String json="{\"model\":\""+escape(model)+"\",\"input\":\""+escape(input)+"\"}";
        HttpRequest request=HttpRequest.newBuilder(URI.create(baseUrl+"/api/embed")).timeout(timeout).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofString());
        String body=response.body();
        if(response.statusCode()/100!=2) {
            String detail = body == null ? "" : body;
            if (detail.contains("does not support embeddings") || response.statusCode() == 500) {
                throw new IllegalStateException("Ollama cannot serve embeddings for '" + model
                        + "'. Upgrade Ollama to v0.11.10 or newer, then run 'ollama pull " + model + "' again.");
            }
            throw new IllegalStateException("Ollama embedding failed (HTTP "+response.statusCode()+"): "+detail);
        }
        String array = embeddingArray(body);
        Matcher m=Pattern.compile("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?").matcher(array); List<Float> values=new ArrayList<>(); while(m.find()) values.add(Float.parseFloat(m.group()));
        if(values.size()!=dimensions) throw new IllegalStateException("Embedding model returned "+values.size()+" dimensions; expected "+dimensions+".");
        return values;
    }
    private static String embeddingArray(String body) {
        int marker = body.indexOf("\"embeddings\"");
        if (marker < 0) marker = body.indexOf("\"embedding\"");
        int start = marker < 0 ? body.indexOf('[') : body.indexOf('[', marker);
        if (start < 0) throw new IllegalStateException("Ollama embedding response did not contain an embedding array.");
        int depth = 0;
        for (int i = start; i < body.length(); i++) {
            char ch = body.charAt(i);
            if (ch == '[') depth++;
            else if (ch == ']' && --depth == 0) return body.substring(start, i + 1);
        }
        throw new IllegalStateException("Ollama returned an unterminated embedding array.");
    }
    private static String escape(String value){return value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");}
}
