package com.deepnote.ai;

import java.util.List;

public interface EmbeddingProvider {
    String modelName();
    int dimensions();
    String configurationVersion();
    List<Float> embedQuery(String query) throws Exception;
    List<Float> embedDocument(String title, String text) throws Exception;
}
