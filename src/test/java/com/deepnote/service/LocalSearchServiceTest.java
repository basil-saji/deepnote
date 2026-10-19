package com.deepnote.service;

import com.deepnote.model.Note;
import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LocalSearchServiceTest {
    @Test void searchesTitleAndContentWithoutCallingAI() {
        Note graph = new Note("Graphs", Path.of("graphs.txt"), "Vertices and edges");
        Note java = new Note("Java", Path.of("java.txt"), "Classes and objects");
        List<Note> matches = new LocalSearchService().search(List.of(graph, java), "VERTICES");
        assertEquals(List.of(graph), matches);
    }
}
