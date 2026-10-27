package com.deepnote.model;
import java.nio.file.Path;
public record VectorSearchResult(Path path, String title, String chunkText, int chunkIndex, double similarity, double score) { }
