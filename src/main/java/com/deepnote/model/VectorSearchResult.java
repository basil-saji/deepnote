package com.deepnote.model;

import java.nio.file.Path;

/** A note-level result; lexical-only matches deliberately have NaN similarity/score. */
public record VectorSearchResult(Path path, String title, String chunkText, int chunkIndex,
                                 double similarity, double score, MatchKind matchKind) {
    public enum MatchKind { SEMANTIC, LEXICAL, BOTH }
    public VectorSearchResult(Path path, String title, String chunkText, int chunkIndex, double similarity, double score) {
        this(path, title, chunkText, chunkIndex, similarity, score, MatchKind.SEMANTIC);
    }
    public boolean hasSemanticScore() { return !Double.isNaN(similarity) && !Double.isNaN(score); }
}
