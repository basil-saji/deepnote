package com.deepnote.service;

import com.deepnote.ai.OllamaEmbeddingClient;
import com.deepnote.config.AppConfig;
import com.deepnote.db.Database;
import com.deepnote.model.Note;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import java.nio.file.*;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

/** Runs against local services when available; otherwise remains an explicit skipped integration test. */
class VectorSearchIntegrationTest {
    @Test void paraphrasedQueryRetrievesExpectedNote() throws Exception {
        AppConfig config=AppConfig.load(); Database database=new Database(config);
        try { database.migrate(); } catch (Exception unavailable) { Assumptions.assumeTrue(false,"PostgreSQL unavailable: "+unavailable.getMessage()); return; }
        Path directory=Files.createTempDirectory("deepnote-vector-it-"); NoteService notes=new NoteService(directory); Note expected=notes.createNote("Database vector retrieval"); expected=notes.saveNote(expected,"PostgreSQL stores vector embeddings so DeepNote can retrieve related notes by semantic meaning."); Note distractor=notes.createNote("Cooking notes"); distractor=notes.saveNote(distractor,"Chop onions and simmer tomatoes before serving the pasta.");
        try {
            var embedder=new OllamaEmbeddingClient(config.ollamaBaseUrl(),config.embeddingModel(),Duration.ofSeconds(config.requestTimeoutSeconds()),config.embeddingDimensions());
            var index=new NoteIndexService(database,embedder,new TextChunker(2048,256)); index.index(expected); index.index(distractor);
            var results=new VectorSearchService(database,embedder,-1.0,directory).search("How does the application find conceptually related notes?",5);
            assertFalse(results.isEmpty()); assertEquals(expected.path(),results.get(0).path());
        } finally { var cleanup=new NoteIndexService(database,new OllamaEmbeddingClient(config.ollamaBaseUrl(),config.embeddingModel(),Duration.ofSeconds(config.requestTimeoutSeconds()),config.embeddingDimensions()),new TextChunker(2048,256)); cleanup.remove(expected.path()); cleanup.remove(distractor.path()); notes.deleteNote(expected); notes.deleteNote(distractor); Files.deleteIfExists(directory); }
    }
}
