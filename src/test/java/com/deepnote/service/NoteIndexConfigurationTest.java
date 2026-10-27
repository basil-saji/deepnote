package com.deepnote.service;

import com.deepnote.ai.OllamaEmbeddingClient;
import com.deepnote.config.AppConfig;
import com.deepnote.db.Database;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class NoteIndexConfigurationTest {
    @Test void modelAndChunkConfigurationChangeInvalidatesIndexVersion() {
        AppConfig c=AppConfig.load(); var db=new Database(c); var e=new OllamaEmbeddingClient(c.ollamaBaseUrl(),c.embeddingModel(),Duration.ofSeconds(c.requestTimeoutSeconds()),c.embeddingDimensions());
        assertNotEquals(new NoteIndexService(db,e,new TextChunker(2048,256)).indexVersion(),new NoteIndexService(db,e,new TextChunker(1024,128)).indexVersion());
    }
}
