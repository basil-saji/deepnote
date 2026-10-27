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
        String array=body.contains("embeddings") ? body.substring(body.indexOf("[[")+1, body.indexOf("]]")) : body.substring(body.indexOf("["), body.lastIndexOf("]")+1);
        Matcher m=Pattern.compile("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?").matcher(array); List<Float> values=new ArrayList<>(); while(m.find()) values.add(Float.parseFloat(m.group()));
        if(values.size()!=dimensions) throw new IllegalStateException("Embedding model returned "+values.size()+" dimensions; expected "+dimensions+".");
        return values;
    }
    private static String escape(String value){return value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");}
}
