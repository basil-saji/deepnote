package com.deepnote.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

/** Ollama EmbeddingGemma retrieval client. Query and document prompts are intentionally asymmetric. */
public final class OllamaEmbeddingClient implements EmbeddingProvider {
    public static final String PROMPT_VERSION = "embeddinggemma-retrieval-v1";
    public static final String QUERY_PREFIX = "task: search result | query: ";
    private static final ObjectMapper JSON = new ObjectMapper();
    private final String baseUrl, model; private final Duration timeout; private final int dimensions; private final HttpClient client;
    public OllamaEmbeddingClient(String baseUrl,String model,Duration timeout,int dimensions){this.baseUrl=baseUrl;this.model=model;this.timeout=timeout;this.dimensions=dimensions;this.client=HttpClient.newBuilder().connectTimeout(timeout).build();}
    @Override public String modelName(){return model;}
    @Override public int dimensions(){return dimensions;}
    @Override public String configurationVersion(){return PROMPT_VERSION+"|"+model+"|"+dimensions;}
    public List<Float> embed(String input)throws Exception{return embedQuery(input);}
    @Override public List<Float> embedQuery(String query)throws Exception{return embedPrepared(QUERY_PREFIX+query);}
    @Override public List<Float> embedDocument(String title,String text)throws Exception{return embedPrepared("title: "+(title==null||title.isBlank()?"none":title)+" | text: "+text);}
    private List<Float> embedPrepared(String input)throws Exception{
        String json=JSON.writeValueAsString(Map.of("model",model,"input",input));
        HttpRequest request=HttpRequest.newBuilder(URI.create(baseUrl+"/api/embed")).timeout(timeout).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build();
        HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString()); String body=response.body()==null?"":response.body();
        if(response.statusCode()/100!=2){if(body.contains("does not support embeddings")||response.statusCode()==500)throw new IllegalStateException("Ollama cannot serve embeddings for '"+model+"'. Upgrade Ollama and pull the model again.");throw new IllegalStateException("Ollama embedding failed (HTTP "+response.statusCode()+"): "+body);}
        JsonNode root;try{root=JSON.readTree(body);}catch(Exception e){throw new IllegalStateException("Ollama returned malformed embedding JSON.",e);}
        JsonNode values=root.path("embeddings");if(values.isArray()&&values.size()>0&&values.get(0).isArray())values=values.get(0);if(!values.isArray()||values.isEmpty()){values=root.path("embedding");}
        if(!values.isArray()||values.size()==0)throw new IllegalStateException("Ollama embedding response did not contain an embedding array.");
        if(values.size()!=dimensions)throw new IllegalStateException("Embedding model returned "+values.size()+" dimensions; expected "+dimensions+".");
        List<Float> result=new ArrayList<>(dimensions);for(JsonNode value:values){if(!value.isNumber()||!Double.isFinite(value.asDouble()))throw new IllegalStateException("Ollama returned a malformed or non-finite embedding value.");result.add(value.floatValue());}return result;
    }
}
