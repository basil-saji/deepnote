package com.deepnote.service;

import com.deepnote.model.VectorSearchResult;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class VectorSearchServiceTest {
    @Test void ranksNotesWithDocumentedChunkFormulaAndCapsDisplayScore() {
        Path path = Path.of("note.txt");
        List<VectorSearchResult> ranked = VectorSearchService.rank(List.of(
                new VectorSearchResult(path, "Note", "best", 0, .8, .8),
                new VectorSearchResult(path, "Note", "second", 1, .6, .6),
                new VectorSearchResult(path, "Note", "third", 2, .4, .4)));
        assertEquals(1, ranked.size());
        assertEquals(.838, ranked.get(0).score(), .0001);
        assertEquals("best", ranked.get(0).chunkText());
    }
}
